/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.file;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.util.ActivityUtils;
import dagger.hilt.android.AndroidEntryPoint;
import lombok.Getter;

import javax.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the files attached to a note and lets the user open or add files.
 *
 * <p>The activity reports whether the note's files were modified when it closes.</p>
 * <p>Returns RESULT_OK if the files were modified, otherwise RESULT_CANCELED.</p>
 */
@AndroidEntryPoint
public final class FilesListActivity extends ActivityBase {

    private static final String TAG = FilesListActivity.class.getSimpleName();

    /**
     * Intent extra containing the ID of the note whose files are displayed.
     */
    public static final String EXTRA_NOTE_ID = "noteId";

    /**
     * Maps each file's decimal ID to its database file ID.
     */
    private final Map<Long, String> filesIdsMap = new HashMap<>();

    /**
     * Service used to retrieve the note associated with this activity.
     */
    @Inject
    NoteService noteService;

    /**
     * Service used to retrieve and save note files.
     */
    @Inject
    FileService fileService;

    /**
     * Factory for themed progress dialogs.
     */
    @Inject
    DialogFactory dialogFactory;

    /**
     * Utility for displaying activity messages.
     */
    @Inject
    ActivityUtils activityUtils;

    /**
     * Launcher used to open a file viewer and receive its result.
     */
    private ActivityResultLauncher<Intent> viewerLauncher;

    /**
     * Launcher used to select files and receive the selection result.
     */
    private ActivityResultLauncher<Intent> filePickerLauncher;

    /**
     * Note whose files are displayed.
     */
    private Note note;

    /**
     * Whether any file was added or modified during this activity.
     */
    @Getter
    private boolean filesModified;

    /**
     * Initializes the activity, validates its note ID, registers result launchers,
     * and loads the note's files.
     *
     * @param savedInstanceState previously saved activity state, if available
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        validateSession();

        setContentView(R.layout.activity_file_list);
        applyInsets(findViewById(R.id.main));

        String noteId;

        try {
            noteId = requireNoteId(getIntent());
        } catch (IllegalStateException e) {
            finishWithError(R.string.files_list_missing_note_id,
                    "Missing note id in FilesListActivity intent", e);
            return;
        }

        viewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getViewerResultCallback());
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getAddFilesResultCallback());

        getOnBackPressedDispatcher()
                .addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        onBackButtonPressed();
                    }
                });

        loadFiles(noteId);
    }

    /**
     * Closes the activity with the appropriate result code.
     */
    @Override
    public void finish() {
        setResult(filesModified ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }

    /**
     * Reads the note ID from an activity intent.
     *
     * @param intent intent expected to contain {@link #EXTRA_NOTE_ID}
     * @return the note ID
     * @throws IllegalStateException if the intent does not contain a note ID
     */
    static String requireNoteId(Intent intent) {
        var noteId = intent.getStringExtra(EXTRA_NOTE_ID);

        if (noteId == null) {
            throw new IllegalStateException("Note id didn't provided");
        }

        return noteId;
    }

    /**
     * Logs an activity error, displays the corresponding message, and finishes.
     *
     * @param messageId  string resource for the user-facing message
     * @param logMessage message to write to the error log
     * @param throwable  cause of the error
     */
    private void finishWithError(int messageId, String logMessage, Throwable throwable) {
        Log.e(TAG, logMessage, throwable);
        activityUtils.showToastMessage(getString(messageId), Toast.LENGTH_SHORT);
        finish();
    }

    /**
     * Handles action-bar menu selections, routing the home button through the
     * activity's modified-result handling.
     *
     * @param item selected menu item
     * @return {@code true} if the item was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackButtonPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the result according to whether files changed, then closes the activity.
     */
    private void onBackButtonPressed() {
        finish();
    }

    /**
     * Loads the note and its files on a worker thread, then updates the UI.
     *
     * @param noteId ID of the note whose files should be loaded
     */
    private void loadFiles(String noteId) {
        var progressDialog =
                dialogFactory.getThemedProgressDialog(R.layout.progress_dialog_loading);
        var handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);
            note = noteService.get(noteId);

            if (note == null) {
                throw new IllegalArgumentException("Note with id " + noteId + " not found");
            }

            long filesCount = fileService.getFilesCount(note.getId());
            runOnUiThread(() -> configureActionBar(filesCount));

            filesIdsMap.clear();

            var filesInfos = fileService.getFilesInfo(note.getId());
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

    /**
     * Updates the action-bar title with the file count and note name.
     *
     * @param filesCount number of files attached to the note
     */
    private void configureActionBar(long filesCount) {
        ActionBar actionBar = getSupportActionBar();
        requireNonNull(actionBar);

        var title = getString(R.string.files_list_action_bar_title_format,
                String.valueOf(filesCount), note.getName());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(title);
    }

    /**
     * Attaches the add-file action to its floating action button.
     */
    private void configureButtons() {
        FloatingActionButton addFileButton = findViewById(R.id.addFileButton);

        addFileButton.setOnClickListener(new AddFileOnClick(this, filePickerLauncher));
    }

    /**
     * Attaches the file-opening action to the files list.
     */
    private void configureFilesListView() {
        ListView filesListView = findViewById(R.id.filesListView);
        filesListView.setOnItemClickListener(
                new OpenFileOnClick(this, viewerLauncher, fileService, filesIdsMap)
        );
    }

    /**
     * Displays the files in the list and toggles the empty-list message.
     *
     * @param filesInfos file information to display
     */
    private void fillFilesListView(List<FileInfo> filesInfos) {
        ListView filesView = findViewById(R.id.filesListView);
        TextView missingFilesLabel = findViewById(R.id.missingFilesLabel);

        var adapter = new FilesListAdapter(
                getApplicationContext(),
                R.layout.files_list_item,
                filesInfos);

        filesView.setAdapter(adapter);

        if (!filesInfos.isEmpty()) {
            missingFilesLabel.setVisibility(View.INVISIBLE);

            filesView.setEnabled(true);
            filesView.setVisibility(View.VISIBLE);
        } else {
            missingFilesLabel.setVisibility(View.VISIBLE);

            filesView.setEnabled(false);
            filesView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Creates a callback that reloads the list when a viewed file was modified.
     *
     * @return callback for results from the file viewer
     */
    ActivityResultCallback<ActivityResult> getViewerResultCallback() {
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

    /**
     * Creates a callback that handles the result of selecting files to add.
     *
     * @return callback for results from the file picker
     */
    ActivityResultCallback<ActivityResult> getAddFilesResultCallback() {
        return result -> {
            int resultCode = result.getResultCode();

            if (resultCode == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    addFiles(result.getData());
                } else {
                    finishWithError(R.string.files_list_missing_file_data,
                            "FilesListActivity add-files result was OK without data",
                            new IllegalStateException(
                                    "Activity result is 'OK', but data not provided"));
                }
            }
        };
    }

    /**
     * Saves the selected files for the current note and refreshes the list.
     *
     * @param data result intent containing the selected file URIs
     */
    private void addFiles(Intent data) {
        var progressDialog = dialogFactory.getThemedProgressDialog(R.layout.progress_dialog_adding);

        newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);

            try {
                fileService.saveFiles(note.getId(), getFileUris(data));
            } catch (IOException | DecryptionFailedException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    finishWithError(R.string.files_list_save_failed,
                            "FilesListActivity failed to save files", e);
                });

                return;
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();
                filesModified = true;
                loadFiles(note.getId());
            });
        });
    }

    /**
     * Extracts all selected URIs from an intent, supporting both multi-select
     * clip data and a single selected item.
     *
     * @param data intent returned by the file picker
     * @return selected file URIs
     */
    List<Uri> getFileUris(Intent data) {
        var result = new ArrayList<Uri>();

        if (data.getClipData() != null) {
            var clipData = data.getClipData();
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
