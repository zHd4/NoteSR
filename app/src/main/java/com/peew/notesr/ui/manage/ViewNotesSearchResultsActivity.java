package com.peew.notesr.ui.manage;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.adapters.NotesListAdapter;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.models.Note;
import com.peew.notesr.models.SearchNotesResults;
import com.peew.notesr.ui.ExtendedAppCompatActivity;
import com.peew.notesr.ui.onclick.NoteOnClick;

import java.util.List;
import java.util.stream.Collectors;

public class ViewNotesSearchResultsActivity extends ExtendedAppCompatActivity {
    private SearchNotesResults results;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes_search_results);

        ListView resultsView = findViewById(R.id.notes_search_results_list_view);

        ActionBar actionBar = getSupportActionBar();
        String actionBarTitleFormat = getResources().getString(R.string.found_n);

        results = (SearchNotesResults) getIntent().getSerializableExtra("results");

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(String.format(actionBarTitleFormat, results.results().size()));

        fillResultsList(resultsView);

        resultsView.setOnItemClickListener(new NoteOnClick(this));
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void fillResultsList(ListView resultsView) {
        NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

        List<Note> notes = results.results().stream()
                .map(notesTable::get)
                .collect(Collectors.toList());

        NotesListAdapter adapter = new NotesListAdapter(
                App.getContext(),
                R.layout.notes_list_item,
                notes);

        resultsView.setAdapter(adapter);
    }
}