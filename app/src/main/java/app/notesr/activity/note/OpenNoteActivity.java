/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.TextViewKt;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.service.file.FileService;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import io.noties.markwon.Markwon;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

public final class OpenNoteActivity extends ActivityBase {
    public static final String EXTRA_NOTE_ID = "noteId";
    public static final String EXTRA_NOTE_MODIFIED = "modified";
    private static final long MAX_COUNT_IN_BADGE = 9;

    private NoteService noteService;
    private FileService fileService;
    private Note note;
    private Menu activityMenu;
    private DialogFactory dialogFactory;
    private boolean isNoteModified;
    private EditText nameField;
    private EditText textField;
    private TextView viewer;
    private ScrollView viewerContainer;
    private Markwon markwon;
    private static final int MODE_EDIT = 0;
    private static final int MODE_TEXT = 1;
    private static final int MODE_MARKDOWN = 2;
    private static final String STATE_VIEW_MODE = "viewMode";
    private int viewMode = MODE_EDIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            viewMode = savedInstanceState.getInt(STATE_VIEW_MODE, MODE_EDIT);
        } else {
            viewMode = MODE_EDIT;
        }

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_open_note);
        applyInsets(findViewById(R.id.main));

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, new FilesUtils());
        dialogFactory = new DialogFactory(this);
        markwon = Markwon.create(this);

        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);

        newSingleThreadExecutor().execute(() -> {
            note = noteService.get(noteId);
            isNoteModified = getIntent().getBooleanExtra(EXTRA_NOTE_MODIFIED, false);

            runOnUiThread(() -> {
                initializeActionBar();
                prepareViews();

                switch (viewMode) {
                    case MODE_TEXT:
                        switchToViewTextMode();
                        break;
                    case MODE_MARKDOWN:
                        switchToViewMarkdownMode();
                        break;
                    case MODE_EDIT:
                    default:
                        switchToEditMode();
                        break;
                }
            });
        });
    }

    private void initializeActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (note != null) {
                actionBar.setTitle(getResources().getString(R.string.edit));
            } else {
                actionBar.setTitle(getResources().getString(R.string.new_note));
            }
        } else {
            throw new NullPointerException("Action bar is null");
        }
    }

    private void prepareViews() {
        nameField = findViewById(R.id.noteNameField);
        textField = findViewById(R.id.noteTextField);
        viewer = findViewById(R.id.viewer);
        viewerContainer = findViewById(R.id.viewerContainer);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        if (note != null) {
            nameField.setText(note.getName());
            textField.setText(note.getText());
        }

        Function1<Editable, Unit> afterTextChangedAction = editable -> {
            if (!isNoteModified) {
                isNoteModified = true;

                MenuItem saveNoteButton = activityMenu.findItem(R.id.saveNoteButton);
                saveNoteButton.setVisible(true);
            }

            return Unit.INSTANCE;
        };

        TextViewKt.doAfterTextChanged(nameField, afterTextChangedAction);
        TextViewKt.doAfterTextChanged(textField, afterTextChangedAction);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_note, menu);
        this.activityMenu = menu;

        MenuItem changeModeButton = menu.findItem(R.id.changeViewModeButton);
        MenuItem saveNoteButton = menu.findItem(R.id.saveNoteButton);
        MenuItem openFilesListButton = menu.findItem(R.id.openFilesListButton);
        MenuItem deleteNoteButton = menu.findItem(R.id.deleteNoteButton);

        changeModeButton.setOnMenuItemClickListener(item -> {
            changeViewModeButtonOnClick();
            return true;
        });

        saveNoteButton.setOnMenuItemClickListener(item -> {
            saveNoteOnClick(nameField, textField);
            return true;
        });

        if (note != null) {
            openFilesListButton.setOnMenuItemClickListener(item -> {
                openFilesListOnClick();
                return true;
            });

            deleteNoteButton.setOnMenuItemClickListener(item -> {
                deleteNoteOnClick();
                return true;
            });

            setAttachedFilesCountBadge(openFilesListButton);
        } else {
            disableMenuItem(openFilesListButton);
            disableMenuItem(deleteNoteButton);
        }

        if (isNoteModified) {
            saveNoteButton.setVisible(true);
        }

        return true;
    }

    private void setAttachedFilesCountBadge(MenuItem openFilesListButton) {
        newSingleThreadExecutor().execute(() -> {
            long filesCount = fileService.getFilesCount(note.getId());

            runOnUiThread(() -> {
                if (filesCount > 0) {
                    openFilesListButton.setActionView(R.layout.button_open_files_list);

                    View view = Objects.requireNonNull(openFilesListButton.getActionView());
                    TextView badge = view.findViewById(R.id.attachedFilesCountBadge);

                    String badgeText = filesCount <= MAX_COUNT_IN_BADGE
                            ? String.valueOf(filesCount)
                            : MAX_COUNT_IN_BADGE + "+";

                    badge.setText(badgeText);
                    badge.setVisibility(View.VISIBLE);

                    view.setOnClickListener(v -> openFilesListOnClick());
                }
            });
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (isNoteModified) {
                Intent intent = new Intent(getApplicationContext(), NotesListActivity.class);
                startActivity(intent);
            } else {
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveNoteOnClick(EditText nameField, EditText textField) {
        String name = nameField.getText().toString();
        String text = textField.getText().toString();

        if (!name.isBlank() && !text.isBlank()) {
            if (note == null) {
                note = new Note();
            }

            note.setName(name);
            note.setText(text);
            note.setUpdatedAt(LocalDateTime.now());

            Dialog progressDialog = dialogFactory
                    .getThemedProgressDialog(R.layout.progress_dialog_loading);

            newSingleThreadExecutor().execute(() -> {
                runOnUiThread(progressDialog::show);
                noteService.save(note);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                });
            });
        }
    }

    private void deleteNoteOnClick() {
        DialogInterface.OnClickListener buttonHandler = deleteNoteDialogOnClick();
        dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.delete, buttonHandler)
                .setNegativeButton(R.string.no, buttonHandler)
                .create()
                .show();
    }

    private void openFilesListOnClick() {
        Intent intent = new Intent(getApplicationContext(), FilesListActivity.class);

        intent.putExtra(FilesListActivity.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Dialog progressDialog = dialogFactory
                        .getThemedProgressDialog(R.layout.progress_dialog_deleting);

                newSingleThreadExecutor().execute(() -> {
                    runOnUiThread(progressDialog::show);

                    try {
                        noteService.delete(note.getId(), fileService);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                    });
                });
            }
        };
    }

    private void changeViewModeButtonOnClick() {
        View anchor = findViewById(R.id.changeViewModeButton);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.menu_note_view_mode);

        Menu popupMenu = popup.getMenu();

        if (viewMode == MODE_EDIT) {
            popupMenu.findItem(R.id.editMenuItem).setChecked(true);
        } else if (viewMode == MODE_TEXT) {
            popupMenu.findItem(R.id.viewTextMenuItem).setChecked(true);
        } else if (viewMode == MODE_MARKDOWN) {
            popupMenu.findItem(R.id.viewMarkdownMenuItem).setChecked(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.editMenuItem) {
                item.setChecked(true);
                viewMode = MODE_EDIT;
                switchToEditMode();
                return true;
            } else if (id == R.id.viewTextMenuItem) {
                item.setChecked(true);
                viewMode = MODE_TEXT;
                switchToViewTextMode();
                return true;
            } else if (id == R.id.viewMarkdownMenuItem) {
                item.setChecked(true);
                viewMode = MODE_MARKDOWN;
                switchToViewMarkdownMode();
                return true;
            }

            return false;
        });

        nameField.post(popup::show);
    }

    private void switchToEditMode() {
        viewMode = MODE_EDIT;
        nameField.setEnabled(false);
        textField.setVisibility(View.VISIBLE);
        viewerContainer.setVisibility(View.GONE);
    }

    private void switchToViewTextMode() {
        viewMode = MODE_TEXT;
        nameField.setEnabled(true);
        textField.setVisibility(View.GONE);

        viewer.setText(textField.getText().toString());
        viewerContainer.setVisibility(View.VISIBLE);
    }

    private void switchToViewMarkdownMode() {
        viewMode = MODE_MARKDOWN;
        nameField.setEnabled(true);
        textField.setVisibility(View.GONE);

        markwon.setMarkdown(viewer, textField.getText().toString());
        viewerContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_VIEW_MODE, viewMode);
    }

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.setVisible(false);
    }
}
