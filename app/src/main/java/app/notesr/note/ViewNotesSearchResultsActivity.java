package app.notesr.note;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.service.note.NoteService;
import app.notesr.model.Note;
import app.notesr.dto.SearchNotesResults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ViewNotesSearchResultsActivity extends ActivityBase {
    private SearchNotesResults results;
    private final Map<Long, String> notesIdsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes_search_results);

        ListView resultsView = findViewById(R.id.notesSearchResultsListView);

        ActionBar actionBar = getSupportActionBar();
        String actionBarTitleFormat = getResources().getString(R.string.found_n);

        results = (SearchNotesResults) getIntent().getSerializableExtra("results");

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(String.format(actionBarTitleFormat, results.results().size()));

        fillResultsList(resultsView);

        resultsView.setOnItemClickListener(new OpenNoteOnClick(this, notesIdsMap));
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void fillResultsList(ListView resultsView) {
        AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
        NoteService noteService = new NoteService(db);

        List<Note> notes = results.results()
                .stream()
                .map(noteService::get)
                .collect(Collectors.toList());

        notes.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));

        NoteListAdapter adapter = new NoteListAdapter(
                App.getContext(),
                R.layout.notes_list_item,
                notes);

        resultsView.setAdapter(adapter);
    }
}