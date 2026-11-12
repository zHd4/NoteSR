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
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Displays the user's notes and provides note search, creation, and app actions.
 */
@AndroidEntryPoint
public final class NotesListActivity extends ActivityBase {

    private static final String TAG = NotesListActivity.class.getSimpleName();

    /** Delay, in milliseconds, before a changed search query is applied. */
    private static final int SEARCH_DELAY = 300;

    /** Actions indexed by their corresponding options-menu item IDs. */
    private final SparseArray<Runnable> menuActions = new SparseArray<>();

    /** Maps each note's decimal ID to its database note ID. */
    private final Map<Long, String> notesIdsMap = new HashMap<>();

    /** Schedules debounced search work on the main thread. */
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    /** Provides app security operations used by this activity. */
    @Inject
    AppSecurityService appSecurityService;

    /** Provides access to stored notes and note searches. */
    @Inject
    NoteService noteService;

    /** Creates themed dialogs displayed by this activity. */
    @Inject
    DialogFactory dialogFactory;

    /** Launches the note editor and receives its result. */
    private ActivityResultLauncher<Intent> noteEditorLauncher;

    /** Locks the app in response to the lock action or back navigation. */
    private LockAction lockAction;

    /** Starts the flow for generating a new encryption key. */
    private GenerateNewKeyAction generateNewKeyAction;

    /**
     * Checks whether this activity is finishing or has been destroyed.
     *
     * @return {@code true} if the activity is finishing or destroyed
     */
    boolean isFinishingOrDestroyed() {
        return isFinishing() || isDestroyed();
    }

    /**
     * Initializes the activity, validates the session, and sets up the notes list and actions.
     *
     * @param savedInstanceState previously saved activity state, or {@code null}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishingOrDestroyed()) {
            return;
        }

        validateSession();

        setContentView(R.layout.activity_note_list);
        applyInsets(findViewById(R.id.main));

        lockAction = new LockAction(this, appSecurityService);
        generateNewKeyAction = new GenerateNewKeyAction(this);

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

    /**
     * Creates the options menu and configures its actions and search view.
     *
     * @param menu the options menu to populate
     * @return {@code true} when the menu is displayed
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_notes_list, menu);

        menuActions.put(R.id.lockAppButton, lockAction::lock);
        menuActions.put(R.id.changePasswordMenuItem, this::startChangePasswordActivity);
        menuActions.put(R.id.rotateKey, generateNewKeyAction::startActivity);
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

    /**
     * Runs the action associated with a selected options-menu item.
     *
     * @param item the selected menu item
     * @return {@code true} if the item was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home || itemId == R.id.searchNotesButton) {
            return super.onOptionsItemSelected(item);
        }

        Runnable action = menuActions.get(itemId);

        if (action == null) {
            return super.onOptionsItemSelected(item);
        }

        action.run();
        return true;
    }

    /**
     * Creates a listener that submits searches immediately and applies edited queries after a
     * short delay.
     *
     * @return the listener for search query changes and submissions
     */
    SearchView.OnQueryTextListener getSearchQueryListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> {
                    if (isFinishingOrDestroyed()) {
                        return;
                    }

                    String query = newText == null || newText.isEmpty() ? null : newText;
                    loadNotes(query);
                }, SEARCH_DELAY);

                return true;
            }
        };
    }

    /**
     * Creates a listener that resets the search and reloads all notes when the search view closes.
     *
     * @return the search view's attachment-state listener
     */
    View.OnAttachStateChangeListener getSearchViewAttachStateChangeListener() {
        return new SearchView.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View view) {
                Log.d(TAG, "SearchView attached");
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View view) {
                if (isFinishingOrDestroyed()) {
                    return;
                }

                SearchView searchView = (SearchView) view;
                searchView.setQuery("", false);
                loadNotes();
                invalidateOptionsMenu();
            }
        };
    }

    /** Loads all notes and displays them in the list. */
    private void loadNotes() {
        loadNotes(null);
    }

    /**
     * Loads notes matching the optional query and updates the list on the UI thread.
     *
     * @param searchQuery query to search for, or {@code null} to load all notes
     */
    private void loadNotes(String searchQuery) {
        if (isFinishingOrDestroyed()) {
            return;
        }

        Dialog progressDialog = dialogFactory
                .getThemedProgressDialog(R.layout.progress_dialog_loading);

        Handler handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);

            List<Note> notes = searchQuery != null
                    ? noteService.search(searchQuery)
                    : noteService.getAll();

            runOnUiThread(() -> {
                notesIdsMap.clear();
                notes.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));

                fillNotesListView(notes);
                progressDialog.dismiss();
            });
        });
    }

    /**
     * Updates the notes list and toggles the empty-state label according to its contents.
     *
     * @param notes notes to display
     */
    private void fillNotesListView(List<Note> notes) {
        ListView notesView = findViewById(R.id.notesListView);
        TextView missingNotesLabel = findViewById(R.id.missingNotesLabel);

        NotesListAdapter adapter = new NotesListAdapter(
                getApplicationContext(),
                R.layout.notes_list_item,
                notes);

        notesView.setAdapter(adapter);

        if (!notes.isEmpty()) {
            missingNotesLabel.setVisibility(View.INVISIBLE);

            notesView.setEnabled(true);
            notesView.setVisibility(View.VISIBLE);
        } else {
            missingNotesLabel.setVisibility(View.VISIBLE);

            notesView.setEnabled(false);
            notesView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Creates the callback that reloads notes after a successful note-editor result.
     *
     * @return the callback for the note editor's activity result
     */
    ActivityResultCallback<ActivityResult> getOpenNoteResultCallback() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                loadNotes();
            }
        };
    }

    /** Opens the note editor to create a new note. */
    private void createNewNote() {
        openNote(null);
    }

    /**
     * Opens the note editor for the specified note, or starts a new note
     * when the ID is {@code null}.
     *
     * @param noteId ID of the note to open, or {@code null} to create a note
     */
    private void openNote(String noteId) {
        Intent intent = new Intent(getApplicationContext(), OpenNoteActivity.class);

        if (noteId != null) {
            intent.putExtra(OpenNoteActivity.EXTRA_NOTE_ID, noteId);
        }

        noteEditorLauncher.launch(intent);
    }

    /** Starts the authentication flow for changing the app password. */
    private void startChangePasswordActivity() {
        Intent intent = new Intent(getApplicationContext(), AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.CHANGE_PASSWORD.getModeName());
        startActivity(intent);
    }

    /**
     * Creates the back-press handler that locks the app instead of leaving the activity.
     *
     * @return the callback registered with the back-press dispatcher
     */
    private OnBackPressedCallback getOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                lockAction.lock();
            }
        };
    }
}
