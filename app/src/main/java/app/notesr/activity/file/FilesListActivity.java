/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.activity.note.OpenNoteActivity;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FilesListActivity extends ActivityBase {
    private final Map<Long, String> filesIdsMap = new HashMap<>();

    private FileService fileService;
    private Note note;
    private boolean isNoteModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        String noteId = getIntent().getStringExtra("noteId");

        if (noteId == null) {
            throw new RuntimeException("Note id didn't provided");
        }

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        fileService = new FileService(context, db, cryptor, new FilesUtils());
        isNoteModified = getIntent().getBooleanExtra("modified", false);

        NoteService noteService = new NoteService(db);

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();
        Handler handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);
            note = noteService.get(noteId);

            if (note == null) {
                throw new RuntimeException("Note with id " + noteId + " not found");
            }

            long filesCount = fileService.getFilesCount(note.getId());
            runOnUiThread(() -> configureActionBar(filesCount));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isNoteModified) {
                Intent intent = new Intent(getApplicationContext(), OpenNoteActivity.class)
                        .putExtra("noteId", note.getId())
                        .putExtra("modified", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);
            } else {
                finish();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void configureActionBar(long filesCount) {
        ActionBar actionBar = getSupportActionBar();
        requireNonNull(actionBar);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("(" + filesCount + ") Files of: " + note.getName());
    }

    private void configureButtons() {
        FloatingActionButton addFileButton = findViewById(R.id.addFileButton);

        addFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), AddFileActivity.class);

            intent.putExtra("noteId", note.getId());
            startActivity(intent);
        });
    }

    private void configureFilesListView() {
        ListView filesListView = findViewById(R.id.filesListView);
        filesListView.setOnItemClickListener(
                new OpenFileOnClick(this, fileService, filesIdsMap)
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
}
