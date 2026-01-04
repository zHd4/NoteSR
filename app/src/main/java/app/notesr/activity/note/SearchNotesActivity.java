/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;

public final class SearchNotesActivity extends ActivityBase {

    private NoteService noteService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);

        noteService = new NoteService(DatabaseProvider.getInstance(getApplicationContext()));

        ActionBar actionBar = getSupportActionBar();
        requireNonNull(actionBar);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.search));

        EditText queryField = findViewById(R.id.searchNotesQueryField);
        findViewById(R.id.searchNotesButton).setOnClickListener(searchButtonOnClick(queryField));
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
                AlertDialog.Builder builder = new AlertDialog.Builder(this,
                        R.style.AlertDialogTheme);
                builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

                Dialog progressDialog = builder.create();

                newSingleThreadExecutor().execute(() -> {
                    runOnUiThread(progressDialog::show);
                    ArrayList<Note> results = search(query);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();

                        Context context = getApplicationContext();
                        Intent intent = new Intent(context, ViewNotesSearchResultsActivity.class)
                                .putExtra("results", results);

                        startActivity(intent);
                    });
                });
            }
        };
    }

    private ArrayList<Note> search(String query) {
        return new ArrayList<>(noteService.search(query));
    }
}
