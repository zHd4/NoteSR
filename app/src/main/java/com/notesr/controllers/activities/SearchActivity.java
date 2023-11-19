package com.notesr.controllers.activities;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.notesr.R;
import com.notesr.controllers.ActivityHelper;
import com.notesr.controllers.NotesController;
import com.notesr.controllers.generators.TablesGenerator;
import com.notesr.models.Note;

import java.util.Arrays;

public class SearchActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ActivityHelper.checkReady(getApplicationContext(), this);

        final ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Search");

        final EditText queryField = findViewById(R.id.searchField);
        Button searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String keyword = queryField.getText().toString();

            if(keyword.length() > 0) {
                try {
                    loadDataToTable(filter(getNotes(), keyword));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                actionBar.setTitle("Search Results");
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private Note[] filter(final Note[] data, final String keyword) {
        Note[] results = new Note[0];

        for(int i = 0; i < data.length; i++) {

            String name = data[i].getName().toLowerCase();
            String text = data[i].getText().toLowerCase();

            if(name.contains(keyword.toLowerCase()) || text.contains(keyword.toLowerCase())) {
                results = Arrays.copyOf(results, results.length + 1);
                results[results.length - 1] = data[i];
            }
        }

        return results;
    }

    private Note[] getNotes() throws Exception {
        return NotesController.getNotes(getApplicationContext());
    }

    private void loadDataToTable(Note[] data) {
        TableLayout table = findViewById(R.id.searchResultsTable);

        table.removeAllViews();

        new TablesGenerator().fillNotesTable(
                this,
                this,
                table,
                data,
                getResources().getColor(R.color.buttonBackground));
    }
}
