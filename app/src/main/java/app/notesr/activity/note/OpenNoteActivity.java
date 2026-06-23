/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.TextViewKt;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
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
    private ActionBar actionBar;
    private Menu activityMenu;
    private DialogFactory dialogFactory;
    private boolean isNoteModified;
    private EditText nameField;
    private EditText textField;
    private TextView markdownViewer;
    private ScrollView markdownViewerContainer;
    private Markwon markwon;
    private static final String STATE_OPEN_MODE = "openMode";
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

            if (isNewNote()) {
                note = new Note();
            }

            isNoteModified = getIntent().getBooleanExtra(EXTRA_NOTE_MODIFIED, false);

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
    }

    private void initializeActionBar() {
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            int titleId = isNewNote() ? R.string.new_note : R.string.edit;
            actionBar.setTitle(getResources().getString(titleId));
        } else {
            throw new NullPointerException("Action bar is null");
        }
    }

    private void prepareViews() {
        nameField = findViewById(R.id.noteNameField);
        textField = findViewById(R.id.noteTextField);
        markdownViewer = findViewById(R.id.markdownViewer);
        markdownViewerContainer = findViewById(R.id.markdownViewerContainer);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        nameField.setText(note.getName());
        textField.setText(note.getText());

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

    @SuppressWarnings("ConstantValue") // Because note id could be null before first save
    private boolean isNewNote() {
        return note == null || note.getId() == null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_note, menu);
        this.activityMenu = menu;

        MenuItem changeModeButton = menu.findItem(R.id.changeOpenModeButton);
        MenuItem saveNoteButton = menu.findItem(R.id.saveNoteButton);
        MenuItem openFilesListButton = menu.findItem(R.id.openFilesListButton);
        MenuItem deleteNoteButton = menu.findItem(R.id.deleteNoteButton);

        changeModeButton.setOnMenuItemClickListener(item -> {
            changeOpenModeButtonOnClick();
            return true;
        });

        saveNoteButton.setOnMenuItemClickListener(new SaveNoteOnClick(this, note,
                noteService, dialogFactory, nameField, textField));

        if (!isNewNote()) {
            openFilesListButton.setOnMenuItemClickListener(
                    new OpenFilesListOnClick(this, note));

            deleteNoteButton.setOnMenuItemClickListener(new DeleteNoteOnClick(this, note,
                    noteService, fileService, dialogFactory));

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
                    view.setOnClickListener(new OpenFilesListOnClick(this, note));
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

    private void changeOpenModeButtonOnClick() {
        View anchor = findViewById(R.id.changeOpenModeButton);
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
        actionBar.setTitle(getResources().getString(R.string.edit));
    }

    private void switchToViewMarkdownMode() {
        openMode = OpenNoteMode.MARKDOWN_VIEW;
        nameField.setEnabled(false);
        textField.setVisibility(View.GONE);

        markwon.setMarkdown(markdownViewer, textField.getText().toString());
        markdownViewer.setMovementMethod(LinkMovementMethod.getInstance());
        markdownViewerContainer.setVisibility(View.VISIBLE);
        actionBar.setTitle(getResources().getString(R.string.view_markdown));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_OPEN_MODE, openMode.getModeCode());
    }

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.setVisible(false);
    }
}
