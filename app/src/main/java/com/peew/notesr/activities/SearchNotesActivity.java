package com.peew.notesr.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import com.peew.notesr.R;

public class SearchNotesActivity extends ExtendedAppCompatActivity {
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
        return view -> {};
    }
}