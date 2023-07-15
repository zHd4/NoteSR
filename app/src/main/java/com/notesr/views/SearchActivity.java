package com.notesr.views;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.notesr.R;
import com.notesr.controllers.NotesController;
import com.notesr.controllers.ActivityTools;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TableLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.controllers.TableGenerator;
import com.notesr.models.Note;

public class SearchActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ActivityTools.checkReady(getApplicationContext(), this);

        final ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Search");

        final EditText queryField = findViewById(R.id.searchField);
        Button searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = queryField.getText().toString();

                if(keyword.length() > 0) {
                    try {
                        loadDataToTable(filter(getNotes(), keyword));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    actionBar.setTitle("Search Results");
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private Note[] filter(final Note[] data, final String keyword) {
        Note[] results = new Note[data.length];

        for(int i = 0; i < data.length; i++) {
            if(data[i].getName().contains(keyword) || data[i].getText().contains(keyword)) {
                results[i] = data[i];
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

        new TableGenerator().fillTable(
                this,
                this,
                table,
                data,
                getResources().getColor(R.color.buttonBackground),
                getWindowManager().getDefaultDisplay().getWidth() / 33
        );
    }
}
