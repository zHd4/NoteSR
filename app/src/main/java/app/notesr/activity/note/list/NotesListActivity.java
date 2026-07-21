/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note.list;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.exporter.ExportActivity;
import app.notesr.activity.importer.ImportActivity;
import app.notesr.activity.note.editor.OpenNoteActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.GenerateNewKeyAction;
import app.notesr.activity.security.LockAction;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NotesListActivity extends ActivityBase {

    private static final int SEARCH_DELAY = 300;

    private final SparseArray<Runnable> menuActions = new SparseArray<>();
    private final Map<Long, String> notesIdsMap = new HashMap<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<Intent> noteEditorLauncher;
    private LockAction lockAction;
    private GenerateNewKeyAction generateNewKeyAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_note_list);
        applyInsets(findViewById(R.id.main));

        var appSecurityService = new AppSecurityService(getApplicationContext());

        lockAction = new LockAction(this, appSecurityService);
        generateNewKeyAction = new GenerateNewKeyAction(this, appSecurityService);

        noteEditorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getOpenNoteResultCallback());

        getOnBackPressedDispatcher().addCallback(this, getOnBackPressedCallback());

        ListView notesView = findViewById(R.id.notesListView);
        FloatingActionButton newNoteButton = findViewById(R.id.addNoteButton);

        loadNotes();

        notesView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    String noteId = notesIdsMap.get(id);
                    openNote(noteId);
                });

        newNoteButton.setOnClickListener(view -> createNewNote());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_notes_list, menu);

        menuActions.put(R.id.lockAppButton, lockAction::lock);
        menuActions.put(R.id.changePasswordMenuItem, this::startChangePasswordActivity);
        menuActions.put(R.id.generateNewKeyMenuItem, generateNewKeyAction::startActivity);
        menuActions.put(R.id.exportMenuItem,
                () -> startActivity(new Intent(this, ExportActivity.class)));
        menuActions.put(R.id.importMenuItem,
                () -> startActivity(new Intent(this, ImportActivity.class)));

        SearchView searchView = (SearchView) menu.findItem(R.id.searchNotesButton).getActionView();
        requireNonNull(searchView, "SearchView is null");

        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(getSearchQueryListener());
        searchView.addOnAttachStateChangeListener(getSearchViewAttachStateChangeListener());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();

        if (itemId != R.id.searchNotesButton) {
            requireNonNull(menuActions.get(itemId)).run();
        }

        return true;
    }

    private SearchView.OnQueryTextListener getSearchQueryListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.postDelayed(() -> {
                    String query = newText == null || newText.isEmpty() ? null : newText;
                    loadNotes(query);
                }, SEARCH_DELAY);

                return true;
            }
        };
    }

    private SearchView.OnAttachStateChangeListener getSearchViewAttachStateChangeListener() {
        return new SearchView.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View view) {
                Log.d("SearchView", "View attached");
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View view) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                SearchView searchView = (SearchView) view;
                searchView.setQuery("", false);
                loadNotes();
                invalidateOptionsMenu();
            }
        };
    }

    private void loadNotes() {
        loadNotes(null);
    }

    private void loadNotes(String searchQuery) {
        Dialog progressDialog = new DialogFactory(this)
                .getThemedProgressDialog(R.layout.progress_dialog_loading);

        AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
        NoteService noteService = new NoteService(db);
        Handler handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);

            List<Note> notes = searchQuery != null
                    ? noteService.search(searchQuery)
                    : noteService.getAll();

            notesIdsMap.clear();
            notes.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));
            runOnUiThread(() -> fillNotesListView(notes));

            progressDialog.dismiss();
        });
    }

    private void fillNotesListView(List<Note> notes) {
        ListView notesView = findViewById(R.id.notesListView);
        TextView missingNotesLabel = findViewById(R.id.missingNotesLabel);

        if (!notes.isEmpty()) {
            missingNotesLabel.setVisibility(View.INVISIBLE);

            notesView.setEnabled(true);
            notesView.setVisibility(View.VISIBLE);

            NotesListAdapter adapter = new NotesListAdapter(
                    getApplicationContext(),
                    R.layout.notes_list_item,
                    notes);

            notesView.setAdapter(adapter);
        } else {
            missingNotesLabel.setVisibility(View.VISIBLE);

            notesView.setEnabled(false);
            notesView.setVisibility(View.INVISIBLE);
        }
    }

    private ActivityResultCallback<ActivityResult> getOpenNoteResultCallback() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                loadNotes();
            }
        };
    }

    private void createNewNote() {
        openNote(null);
    }

    private void openNote(String noteId) {
        Intent intent = new Intent(getApplicationContext(), OpenNoteActivity.class);

        if (noteId != null) {
            intent.putExtra(OpenNoteActivity.EXTRA_NOTE_ID, noteId);
        }

        noteEditorLauncher.launch(intent);
    }

    private void startChangePasswordActivity() {
        Intent intent = new Intent(getApplicationContext(), AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.CHANGE_PASSWORD.toString());
        startActivity(intent);
    }

    private OnBackPressedCallback getOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                lockAction.lock();
            }
        };
    }
}
