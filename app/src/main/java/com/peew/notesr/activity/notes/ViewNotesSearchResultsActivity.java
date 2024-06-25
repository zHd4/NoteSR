package com.peew.notesr.activity.notes;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.adapter.NotesListAdapter;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.Note;
import com.peew.notesr.model.SearchNotesResults;
import com.peew.notesr.onclick.notes.OpenNoteOnClick;

import java.util.List;
import java.util.stream.Collectors;

public class ViewNotesSearchResultsActivity extends AppCompatActivityExtended {
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

        resultsView.setOnItemClickListener(new OpenNoteOnClick(this));
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void fillResultsList(ListView resultsView) {
        NotesTable notesTable = App.getAppContainer().getNotesDatabase().getTable(NotesTable.class);

        List<Note> notes = NotesCrypt.decrypt(results.results().stream()
                .map(notesTable::get)
                .collect(Collectors.toList()));

        NotesListAdapter adapter = new NotesListAdapter(
                App.getContext(),
                R.layout.notes_list_item,
                notes);

        resultsView.setAdapter(adapter);
    }
}