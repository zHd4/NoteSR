/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.file.viewer.FileViewerActivityBase;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FilesListActivity extends ActivityBase {

    public static final String EXTRA_NOTE_ID = "noteId";
    private final Map<Long, String> filesIdsMap = new HashMap<>();

    private NoteService noteService;
    private FileService fileService;
    private ActivityResultLauncher<Intent> viewerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Note note;

    private boolean filesModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_file_list);
        applyInsets(findViewById(R.id.main));

        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);

        if (noteId == null) {
            throw new RuntimeException("Note id didn't provided");
        }

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        fileService = new FileService(context, db, cryptor, new FilesUtils());
        noteService = new NoteService(db);
        viewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getViewerResultCallback());
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getAddFilesResultCallback());

        loadFiles(noteId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(filesModified ? RESULT_OK : RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFiles(String noteId) {
        Dialog progressDialog = new DialogFactory(this)
                .getThemedProgressDialog(R.layout.progress_dialog_loading);

        Handler handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);
            note = noteService.get(noteId);

            if (note == null) {
                throw new RuntimeException("Note with id " + noteId + " not found");
            }

            long filesCount = fileService.getFilesCount(note.getId());
            runOnUiThread(() -> configureActionBar(filesCount));

            filesIdsMap.clear();

            List<FileInfo> filesInfos = fileService.getFilesInfo(note.getId());
            filesInfos.forEach(fileInfo ->
                    filesIdsMap.put(fileInfo.getDecimalId(), fileInfo.getId()));

            progressDialog.dismiss();

            runOnUiThread(() -> {
                fillFilesListView(filesInfos);
                configureFilesListView();
                configureButtons();
            });
        });
    }

    private void configureActionBar(long filesCount) {
        ActionBar actionBar = getSupportActionBar();
        requireNonNull(actionBar);

        String title = getString(R.string.files_list_action_bar_title_format,
                String.valueOf(filesCount), note.getName());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(title);
    }

    private void configureButtons() {
        FloatingActionButton addFileButton = findViewById(R.id.addFileButton);

        addFileButton.setOnClickListener(new AddFileOnClick(this, filePickerLauncher));
    }

    private void configureFilesListView() {
        ListView filesListView = findViewById(R.id.filesListView);
        filesListView.setOnItemClickListener(
                new OpenFileOnClick(this, viewerLauncher, fileService, filesIdsMap)
        );
    }

    private void fillFilesListView(List<FileInfo> filesInfos) {
        ListView filesView = findViewById(R.id.filesListView);
        TextView missingFilesLabel = findViewById(R.id.missingFilesLabel);

        if (!filesInfos.isEmpty()) {
            missingFilesLabel.setVisibility(View.INVISIBLE);
            FilesListAdapter adapter = new FilesListAdapter(
                    getApplicationContext(),
                    R.layout.files_list_item,
                    filesInfos);

            filesView.setAdapter(adapter);
        }
    }

    private ActivityResultCallback<ActivityResult> getViewerResultCallback() {
        return result -> {
            if (result.getResultCode() == FilesListActivity.RESULT_OK) {
                var data = result.getData();

                if (data != null && data.hasExtra(FileViewerActivityBase.EXTRA_FILE_MODIFIED)) {
                    boolean fileModified = data.getBooleanExtra(
                            FileViewerActivityBase.EXTRA_FILE_MODIFIED, false);

                    if (fileModified) {
                        loadFiles(note.getId());
                        filesModified = true;
                    }
                }
            }
        };
    }

    private ActivityResultCallback<ActivityResult> getAddFilesResultCallback() {
        return result -> {
            int resultCode = result.getResultCode();

            if (resultCode == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    addFiles(result.getData());
                    loadFiles(note.getId());

                    filesModified = true;
                } else {
                    throw new IllegalStateException("Activity result is 'OK'" +
                            ", but data not provided");
                }
            }
        };
    }

    private void addFiles(Intent data) {
        Dialog progressDialog = new DialogFactory(this)
                .getThemedProgressDialog(R.layout.progress_dialog_adding);

        newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);

            try {
                fileService.saveFiles(note.getId(), getFileUris(data));
            } catch (IOException | DecryptionFailedException e) {
                throw new RuntimeException(e);
            }

            runOnUiThread(progressDialog::dismiss);
        });
    }

    private List<Uri> getFileUris(Intent data) {
        List<Uri> result = new ArrayList<>();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int filesCount = clipData.getItemCount();

            for (int i = 0; i < filesCount; i++) {
                result.add(clipData.getItemAt(i).getUri());
            }
        } else {
            result.add(data.getData());
        }

        return result;
    }
}
