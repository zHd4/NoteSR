package app.notesr.activity.notes;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.ActionBar;
import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.adapter.NotesListAdapter;
import app.notesr.service.NoteService;
import app.notesr.dto.Note;
import app.notesr.dto.SearchNotesResults;
import app.notesr.onclick.notes.OpenNoteOnClick;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ViewNotesSearchResultsActivity extends ExtendedAppCompatActivity {
    private SearchNotesResults results;
    private final Map<Long, String> notesIdsMap = new HashMap<>();

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

        resultsView.setOnItemClickListener(new OpenNoteOnClick(this, notesIdsMap));
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void fillResultsList(ListView resultsView) {
        NoteService service = App.getAppContainer().getNoteService();

        List<Note> notes = results.results()
                .stream()
                .map(service::get)
                .collect(Collectors.toList());

        notes.forEach(note -> notesIdsMap.put(note.getDecimalId(), note.getId()));

        NotesListAdapter adapter = new NotesListAdapter(
                App.getContext(),
                R.layout.notes_list_item,
                notes);

        resultsView.setAdapter(adapter);
    }
}