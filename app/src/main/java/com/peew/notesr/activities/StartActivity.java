package com.peew.notesr.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.peew.notesr.App;
import com.peew.notesr.R;

public class StartActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);
        View.OnClickListener onGetStartedButtonClick = view -> startActivity(setupKeyActivityIntent);

        findViewById(R.id.get_started_button).setOnClickListener(onGetStartedButtonClick);
    }
}
