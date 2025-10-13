package app.notesr.note.activity;

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
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewKt;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.file.activity.FilesListActivity;
import app.notesr.file.service.FileService;
import app.notesr.data.model.Note;
import app.notesr.note.service.NoteService;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

public final class OpenNoteActivity extends ActivityBase {
    private static final long MAX_COUNT_IN_BADGE = 9;
    private final Map<Integer, Consumer<?>> menuItemsMap = new HashMap<>();

    private NoteService noteService;
    private FileService fileService;
    private Note note;
    private Menu activityMenu;
    private boolean isNoteModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_note);

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, new FilesUtils());

        String noteId = getIntent().getStringExtra("noteId");

        newSingleThreadExecutor().execute(() -> {
            note = noteService.get(noteId);
            isNoteModified = getIntent().getBooleanExtra("modified", false);

            runOnUiThread(() -> {
                initializeActionBar();
                prepareEditorFields();
            });
        });
    }

    private void initializeActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (note != null) {
                actionBar.setTitle(getResources().getString(R.string.edit_note));
            } else {
                actionBar.setTitle(getResources().getString(R.string.new_note));
            }
        } else {
            throw new NullPointerException("Action bar is null");
        }
    }

    private void prepareEditorFields() {
        EditText nameField = findViewById(R.id.noteNameField);
        EditText textField = findViewById(R.id.noteTextField);

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
        EditText nameField = findViewById(R.id.noteNameField);
        EditText textField = findViewById(R.id.noteTextField);

        getMenuInflater().inflate(R.menu.menu_open_note, menu);
        this.activityMenu = menu;

        MenuItem saveNoteButton = menu.findItem(R.id.saveNoteButton);
        MenuItem openFilesListButton = menu.findItem(R.id.openFilesListButton);
        MenuItem deleteNoteButton = menu.findItem(R.id.deleteNoteButton);

        menuItemsMap.put(saveNoteButton.getItemId(),
                action -> saveNoteOnClick(nameField, textField));

        if (note != null) {
            menuItemsMap.put(openFilesListButton.getItemId(),
                    action -> openFilesListOnClick());

            menuItemsMap.put(deleteNoteButton.getItemId(),
                    action -> deleteNoteOnClick());

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

        Consumer<?> action = menuItemsMap.get(id);

        if (action != null) {
            action.accept(null);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                    R.style.AlertDialogTheme);
            builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

            Dialog progressDialog = builder.create();

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_action_cannot_be_undo);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.delete, deleteNoteDialogOnClick());
        builder.setNegativeButton(R.string.no, deleteNoteDialogOnClick());

        builder.create().show();
    }

    private void openFilesListOnClick() {
        Intent intent = new Intent(getApplicationContext(), FilesListActivity.class);

        intent.putExtra("noteId", note.getId());
        startActivity(intent);
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this,
                        R.style.AlertDialogTheme);
                builder.setView(R.layout.progress_dialog_deleting).setCancelable(false);

                AlertDialog progressDialog = builder.create();

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

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.setVisible(false);
    }
}
