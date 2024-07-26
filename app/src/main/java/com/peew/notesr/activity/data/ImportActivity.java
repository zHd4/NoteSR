package com.peew.notesr.activity.data;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;

public class ImportActivity extends ExtendedAppCompatActivity {

    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        actionBar = getSupportActionBar();
        assert actionBar != null;
    }
}