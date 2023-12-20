package com.peew.notesr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.peew.notesr.R;
import com.peew.notesr.models.SearchNotesResults;

public class ViewNotesSearchResultsActivity extends AppCompatActivity {
    private SearchNotesResults results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes_search_results);

        results = (SearchNotesResults) getIntent().getSerializableExtra("results");
    }
}