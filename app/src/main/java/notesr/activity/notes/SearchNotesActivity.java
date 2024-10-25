package notesr.activity.notes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import notesr.App;
import notesr.R;
import notesr.activity.ExtendedAppCompatActivity;
import notesr.manager.NotesManager;
import notesr.model.SearchNotesResults;

public class SearchNotesActivity extends ExtendedAppCompatActivity {
    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.search));

        EditText queryField = findViewById(R.id.searchNotesQueryField);
        findViewById(R.id.searchNotesButton).setOnClickListener(searchButtonOnClick(queryField));
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private View.OnClickListener searchButtonOnClick(EditText queryField) {
        return view -> {
            String query = queryField.getText().toString();

            if (!query.isBlank()) {
                SearchNotesResults results = search(query);
                Intent intent = new Intent(App.getContext(), ViewNotesSearchResultsActivity.class);

                intent.putExtra("results", results);
                startActivity(intent);
            }
        };
    }

    private SearchNotesResults search(String query) {
        NotesManager manager = App.getAppContainer().getNotesManager();
        return new SearchNotesResults(manager.search(query));
    }
}