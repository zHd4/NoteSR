/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.exporter.ExportActivity;
import app.notesr.activity.importer.ImportActivity;
import app.notesr.activity.security.ChangePasswordOnClick;
import app.notesr.activity.security.GenerateNewKeyOnClick;
import app.notesr.activity.security.LockOnClick;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class NotesListActivity extends ActivityBase {

    private final int SEARCH_DELAY = 300;
    private final Map<Integer, Consumer<ActivityBase>> menuItemsMap = new HashMap<>();
    private final Map<Long, String> notesIdsMap = new HashMap<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        applyInsets(findViewById(R.id.main));

        getOnBackPressedDispatcher()
                .addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        LockOnClick lock = new LockOnClick();
                        lock.accept(NotesListActivity.this);
                    }
                });

        ListView notesView = findViewById(R.id.notesListView);
        FloatingActionButton newNoteButton = findViewById(R.id.addNoteButton);

        loadNotes();

        notesView.setOnItemClickListener(new OpenNoteOnClick(this, notesIdsMap));
        newNoteButton.setOnClickListener((view) ->
                startActivity(new Intent(getApplicationContext(), OpenNoteActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Intent importActivityIntent = new Intent(this, ImportActivity.class);
        Intent exportActivityIntent = new Intent(this, ExportActivity.class);

        getMenuInflater().inflate(R.menu.menu_notes_list, menu);

        menuItemsMap.put(R.id.lockAppButton, new LockOnClick());
        menuItemsMap.put(R.id.changePasswordMenuItem, new ChangePasswordOnClick());
        menuItemsMap.put(R.id.generateNewKeyMenuItem, new GenerateNewKeyOnClick());

        menuItemsMap.put(R.id.exportMenuItem, action ->
                startActivity(exportActivityIntent));

        menuItemsMap.put(R.id.importMenuItem, action ->
                startActivity(importActivityIntent));

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
            requireNonNull(menuItemsMap.get(itemId)).accept(this);
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
}
