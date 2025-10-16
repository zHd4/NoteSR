package app.notesr.activity.note;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.data.model.Note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ViewNotesSearchResultsActivity extends ActivityBase {
    private List<Note> results;
    private final Map<Long, String> notesIdsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes_search_results);

        ListView resultsView = findViewById(R.id.notesSearchResultsListView);

        results = (ArrayList<Note>) getIntent().getSerializableExtra("results");

        ActionBar actionBar = getSupportActionBar();
        String actionBarTitleFormat = getResources().getString(R.string.found_n);

        requireNonNull(actionBar);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(String.format(actionBarTitleFormat, results.size()));

        fillResultsList(resultsView);

        resultsView.setOnItemClickListener(new OpenNoteOnClick(this, notesIdsMap));
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void fillResultsList(ListView resultsView) {
        results.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));

        NotesListAdapter adapter = new NotesListAdapter(
                getApplicationContext(),
                R.layout.notes_list_item,
                results);

        resultsView.setAdapter(adapter);
    }
}
