/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note.editor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.TextViewKt;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.file.FilesListActivity;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import app.notesr.core.util.FilesUtils;
import app.notesr.service.security.AppSecurityService;
import io.noties.markwon.Markwon;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.util.Objects;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public final class OpenNoteActivity extends ActivityBase {
    public static final String EXTRA_NOTE_ID = "noteId";
    private static final String STATE_OPEN_MODE = "openMode";
    private static final long MAX_COUNT_IN_BADGE = 9;

    private NoteService noteService;
    private FileService fileService;
    private Note note;

    private ActionBar actionBar;
    private Menu menu;
    private DialogFactory dialogFactory;
    private EditText nameField;
    private EditText textField;
    private TextView markdownViewer;
    private ScrollView markdownViewerContainer;

    private Markwon markwon;
    private ActivityResultLauncher<Intent> openFilesListLauncher;

    private SaveNoteAction saveNoteAction;
    private DeleteNoteAction deleteNoteAction;

    private boolean isNoteFieldsModified;
    private boolean isAttachedFilesModified;
    private OpenNoteMode openMode = OpenNoteMode.EDIT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            openMode = OpenNoteMode.fromCode(savedInstanceState.getInt(STATE_OPEN_MODE));
        } else {
            openMode = OpenNoteMode.EDIT;
        }

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_open_note);
        applyInsets(findViewById(R.id.main));

        var context = getApplicationContext();
        var db = DatabaseProvider.getInstance(context);

        AesCryptor cryptor = AesCryptorFactory.createAesGcmCryptor(
                new AppSecurityService(context).getActualSecrets());

        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, new FilesUtils());
        dialogFactory = new DialogFactory(this);
        markwon = Markwon.create(this);
        openFilesListLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                getFilesListResultCallback());

        var noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);

        newSingleThreadExecutor().execute(() -> {
            note = noteService.get(noteId);

            if (isNewNote()) {
                note = new Note();
            }

            runOnUiThread(() -> {
                initializeActionBar();
                prepareViews();

                switch (openMode) {
                    case MARKDOWN_VIEW:
                        switchToViewMarkdownMode();
                        break;
                    case EDIT:
                    default:
                        switchToEditMode();
                        break;
                }
            });
        });

        getOnBackPressedDispatcher()
                .addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        backButtonOnClick();
                    }
                });
    }

    private void initializeActionBar() {
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getActionBarTitle());
        } else {
            throw new NullPointerException("Action bar is null");
        }
    }

    private void prepareViews() {
        nameField = findViewById(R.id.noteNameField);
        textField = findViewById(R.id.noteTextField);
        markdownViewer = findViewById(R.id.markdownViewer);
        markdownViewerContainer = findViewById(R.id.markdownViewerContainer);

        saveNoteAction = new SaveNoteAction(this, note, noteService, dialogFactory,
                nameField, textField);

        deleteNoteAction = new DeleteNoteAction(this, note, noteService, fileService,
                dialogFactory);


        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        nameField.setText(note.getName());
        textField.setText(note.getText());

        Function1<Editable, Unit> afterTextChangedAction = editable -> {
            onFormTextChanged();
            return Unit.INSTANCE;
        };

        TextViewKt.doAfterTextChanged(nameField, afterTextChangedAction);
        TextViewKt.doAfterTextChanged(textField, afterTextChangedAction);
    }

    @SuppressWarnings("ConstantValue") // Because note id could be null before first save
    private boolean isNewNote() {
        return note == null || note.getId() == null;
    }

    private void saveAndExit() {
        saveNoteAction.execute();
        setResult(RESULT_OK);
        finish();
    }

    private void deleteNoteAndExit() {
        deleteNoteAction.execute(() -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void exitWithoutSaving() {
        setResult(isAttachedFilesModified ? RESULT_OK : RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_note, menu);
        this.menu = menu;

        var changeModeButton = menu.findItem(R.id.changeOpenModeButton);
        var saveNoteButton = menu.findItem(R.id.saveNoteButton);
        var openFilesListButton = menu.findItem(R.id.openFilesListButton);
        var deleteNoteButton = menu.findItem(R.id.deleteNoteButton);

        changeModeButton.setOnMenuItemClickListener(item -> {
            changeOpenModeButtonOnClick();
            return true;
        });

        saveNoteButton.setOnMenuItemClickListener((item) -> {
            if (saveNoteAction.isFormCorrect()) {
                saveAndExit();
            }

            return true;
        });

        if (!isNewNote()) {
            openFilesListButton.setOnMenuItemClickListener(item -> {
                openFilesList();
                return true;
            });

            deleteNoteButton.setOnMenuItemClickListener(item -> {
                deleteNoteAndExit();
                return true;
            });

            setAttachedFilesCountBadge(openFilesListButton);
        } else {
            disableMenuItem(openFilesListButton);
            disableMenuItem(deleteNoteButton);
        }

        if (isNoteFieldsModified) {
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

                    var badgeText = filesCount <= MAX_COUNT_IN_BADGE
                            ? String.valueOf(filesCount)
                            : MAX_COUNT_IN_BADGE + "+";

                    badge.setText(badgeText);
                    badge.setVisibility(View.VISIBLE);
                    view.setOnClickListener(v -> openFilesList());
                }
            });
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            backButtonOnClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void backButtonOnClick() {
        if (isNoteFieldsModified) {
            if (!saveNoteAction.isFormCorrect()) {
                exitWithoutSaving();
                return;
            }

            DialogInterface.OnClickListener buttonHandler = (dialog, result) -> {
                if (result == DialogInterface.BUTTON_POSITIVE) {
                    saveAndExit();
                } else if (result == DialogInterface.BUTTON_NEUTRAL) {
                    exitWithoutSaving();
                }
            };

            dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_unsaved_note_changes)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.save, buttonHandler)
                    .setNeutralButton(R.string.dont_save, buttonHandler)
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
        } else {
            exitWithoutSaving();
        }
    }

    private void openFilesList() {
        var intent = new Intent(getApplicationContext(), FilesListActivity.class)
                .putExtra(FilesListActivity.EXTRA_NOTE_ID, note.getId());

        openFilesListLauncher.launch(intent);
    }

    private void onFormTextChanged() {
        if (!isNoteFieldsModified) {
            isNoteFieldsModified = true;
        }

        var saveNoteButton = menu.findItem(R.id.saveNoteButton);
        boolean isSaveAllowed = isNoteFieldsModified && saveNoteAction.isFormCorrect();

        saveNoteButton.setVisible(isSaveAllowed);
    }

    private ActivityResultCallback<ActivityResult> getFilesListResultCallback() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                isAttachedFilesModified = true;
                setAttachedFilesCountBadge(menu.findItem(R.id.openFilesListButton));
            }
        };
    }

    private void changeOpenModeButtonOnClick() {
        View anchor = findViewById(R.id.popupMenuAnchor);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.menu_open_node_open_mode);

        Menu popupMenu = popup.getMenu();

        if (openMode == OpenNoteMode.EDIT) {
            popupMenu.findItem(R.id.editMenuItem).setChecked(true);
        } else if (openMode == OpenNoteMode.MARKDOWN_VIEW) {
            popupMenu.findItem(R.id.viewMarkdownMenuItem).setChecked(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.editMenuItem) {
                item.setChecked(true);
                openMode = OpenNoteMode.EDIT;
                switchToEditMode();
                return true;
            } else if (id == R.id.viewMarkdownMenuItem) {
                item.setChecked(true);
                openMode = OpenNoteMode.MARKDOWN_VIEW;
                switchToViewMarkdownMode();
                return true;
            }

            return false;
        });

        nameField.post(popup::show);
    }

    private void switchToEditMode() {
        openMode = OpenNoteMode.EDIT;
        nameField.setEnabled(true);
        textField.setVisibility(View.VISIBLE);
        markdownViewerContainer.setVisibility(View.GONE);
        actionBar.setTitle(getActionBarTitle());
    }

    private void switchToViewMarkdownMode() {
        openMode = OpenNoteMode.MARKDOWN_VIEW;
        nameField.setEnabled(false);
        textField.setVisibility(View.GONE);

        markwon.setMarkdown(markdownViewer, textField.getText().toString());
        markdownViewer.setMovementMethod(LinkMovementMethod.getInstance());
        markdownViewerContainer.setVisibility(View.VISIBLE);
        actionBar.setTitle(getActionBarTitle());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_OPEN_MODE, openMode.getModeCode());
    }

    private String getActionBarTitle() {
        return switch (openMode) {
            case EDIT -> isNewNote()
                    ? getResources().getString(R.string.new_note)
                    : getResources().getString(R.string.edit);
            case MARKDOWN_VIEW -> getResources().getString(R.string.view_markdown);
        };
    }

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.setVisible(false);
    }
}
