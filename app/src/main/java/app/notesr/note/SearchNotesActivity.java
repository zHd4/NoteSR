package app.notesr.note;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import java.util.LinkedList;
import java.util.concurrent.Executors;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.db.DatabaseProvider;
import app.notesr.model.Note;
import app.notesr.service.note.NoteService;

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
                Executors.newSingleThreadExecutor().execute(() -> {
                    LinkedList<Note> results = search(query);

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

    private LinkedList<Note> search(String query) {
        return new LinkedList<>(noteService.search(query));
    }
}