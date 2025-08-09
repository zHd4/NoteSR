package app.notesr.note.activity;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.db.DatabaseProvider;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;

public class SearchNotesActivity extends ActivityBase {

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
                newSingleThreadExecutor().execute(() -> {
                    ArrayList<Note> results = search(query);

                    runOnUiThread(() -> {
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