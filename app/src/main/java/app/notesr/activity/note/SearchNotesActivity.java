/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;

public final class SearchNotesActivity extends ActivityBase {

    private NoteService noteService;
    private final Map<Long, String> notesIdsMap = new HashMap<>();
    private ListView resultsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);
        applyInsets(findViewById(R.id.main));

        noteService = new NoteService(DatabaseProvider.getInstance(getApplicationContext()));

        ActionBar actionBar = getSupportActionBar();
        requireNonNull(actionBar);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.search));

        EditText queryField = findViewById(R.id.searchNotesQueryField);
        resultsView = findViewById(R.id.notesSearchResultsListView);

        findViewById(R.id.searchNotesButton).setOnClickListener(searchButtonOnClick(queryField));

        resultsView.setOnItemClickListener(new OpenNoteOnClick(this, notesIdsMap));
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private View.OnClickListener searchButtonOnClick(EditText queryField) {
        return view -> {
            String query = queryField.getText().toString();

            if (!query.isBlank()) {
                Dialog progressDialog = new DialogFactory(this)
                        .getThemedProgressDialog(R.layout.progress_dialog_loading);

                newSingleThreadExecutor().execute(() -> {
                    runOnUiThread(progressDialog::show);
                    ArrayList<Note> results = search(query);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        displayResults(results);
                    });
                });
            }
        };
    }

    private void displayResults(List<Note> results) {
        notesIdsMap.clear();
        results.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));

        NotesListAdapter adapter = new NotesListAdapter(
                getApplicationContext(),
                R.layout.notes_list_item,
                results);

        resultsView.setAdapter(adapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String actionBarTitleFormat = getResources().getString(R.string.found_n);
            actionBar.setTitle(String.format(actionBarTitleFormat, results.size()));
        }
    }

    private ArrayList<Note> search(String query) {
        return new ArrayList<>(noteService.search(query));
    }
}
