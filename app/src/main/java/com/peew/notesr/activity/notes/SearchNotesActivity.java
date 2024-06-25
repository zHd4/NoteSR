package com.peew.notesr.activity.notes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.Note;
import com.peew.notesr.model.SearchNotesResults;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchNotesActivity extends AppCompatActivityExtended {
    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.search));

        EditText queryField = findViewById(R.id.search_notes_query_field);
        findViewById(R.id.search_notes_button).setOnClickListener(searchButtonOnClick(queryField));
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private View.OnClickListener searchButtonOnClick(EditText queryField) {
        return view -> {
            String query = formatValue(queryField.getText().toString());

            if (!query.isBlank()) {
                SearchNotesResults results = search(query);

                Class<?> viewResultsClass = ViewNotesSearchResultsActivity.class;
                Intent viewResultsIntent = new Intent(App.getContext(), viewResultsClass);

                viewResultsIntent.putExtra("results", results);
                startActivity(viewResultsIntent);
            }
        };
    }

    private SearchNotesResults search(String query) {
        NotesTable notesTable = App.getAppContainer().getNotesDatabase().getTable(NotesTable.class);
        List<Note> notes = NotesCrypt.decrypt(notesTable.getAll());

        Predicate<Note> check = note -> {
            boolean foundInName = formatValue(note.getName()).contains(query);
            boolean foundInText = formatValue(note.getText()).contains(query);

            return foundInName || foundInText;
        };

        List<Long> notesIds = notes.stream()
                .filter(check)
                .map(Note::getId)
                .collect(Collectors.toList());

        return new SearchNotesResults(notesIds);
    }

    private String formatValue(String value) {
        return value.toLowerCase().replace("\n", "");
    }
}