package com.peew.notesr.activity.notes;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.adapter.NotesListAdapter;
import com.peew.notesr.manager.NotesManager;
import com.peew.notesr.model.Note;
import com.peew.notesr.model.SearchNotesResults;
import com.peew.notesr.onclick.notes.OpenNoteOnClick;

import java.util.List;
import java.util.stream.Collectors;

public class ViewNotesSearchResultsActivity extends ExtendedAppCompatActivity {
    private SearchNotesResults results;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes_search_results);

        ListView resultsView = findViewById(R.id.notesSearchResultsListView);

        ActionBar actionBar = getSupportActionBar();
        String actionBarTitleFormat = getResources().getString(R.string.found_n);

        //noinspection deprecation
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
        NotesManager manager = App.getAppContainer().getNotesManager();

        List<Note> notes = results.results()
                .stream()
                .map(manager::get)
                .collect(Collectors.toList());

        NotesListAdapter adapter = new NotesListAdapter(
                App.getContext(),
                R.layout.notes_list_item,
                notes);

        resultsView.setAdapter(adapter);
    }
}