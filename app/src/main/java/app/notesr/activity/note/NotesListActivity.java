/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity.note;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
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
import java.util.Objects;
import java.util.function.Consumer;

public final class NotesListActivity extends ActivityBase {
    private final Map<Integer, Consumer<ActivityBase>> menuItemsMap = new HashMap<>();
    private final Map<Long, String> notesIdsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

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
        Intent searchActivityIntent = new Intent(this, SearchNotesActivity.class);
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

        menuItemsMap.put(R.id.searchMenuItem, action ->
                startActivity(searchActivityIntent));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Objects.requireNonNull(menuItemsMap.get(id)).accept(this);

        return true;
    }

    private void loadNotes() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);

        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
        NoteService noteService = new NoteService(db);
        Handler handler = new Handler(Looper.getMainLooper());

        newSingleThreadExecutor().execute(() -> {
            handler.post(progressDialog::show);

            List<Note> notes = noteService.getAll();

            notes.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));
            fillNotesListView(notes);

            progressDialog.dismiss();
        });
    }

    private void fillNotesListView(List<Note> notes) {
        ListView notesView = findViewById(R.id.notesListView);
        TextView missingNotesLabel = findViewById(R.id.missingNotesLabel);

        if (!notes.isEmpty()) {
            missingNotesLabel.setVisibility(View.INVISIBLE);
            NotesListAdapter adapter = new NotesListAdapter(
                    getApplicationContext(),
                    R.layout.notes_list_item,
                    notes);

            runOnUiThread(() -> notesView.setAdapter(adapter));
        }
    }
}
