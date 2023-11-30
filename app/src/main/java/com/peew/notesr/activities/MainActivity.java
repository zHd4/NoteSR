package com.peew.notesr.activities;

import android.content.Intent;
import android.os.Bundle;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;

public class MainActivity extends ExtendedAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(CryptoManager.getInstance().isFirstRun()) {
            startActivity(new Intent(App.getContext(), StartActivity.class));
        }
    }
}