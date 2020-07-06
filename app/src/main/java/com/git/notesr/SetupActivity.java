package com.git.notesr;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class SetupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        Button get_started_button = findViewById(R.id.getStartedButton);
        get_started_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetStartedClicked();
            }
        });
    }

    private void GetStartedClicked(){
        Intent saIntent = new Intent(this, GenkeysActivity.class);
        startActivity(saIntent);
    }
}