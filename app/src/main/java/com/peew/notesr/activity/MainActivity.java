package com.peew.notesr.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.data.ExportActivity;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.activity.security.AuthActivity;
import com.peew.notesr.activity.security.KeyRecoveryActivity;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.service.ExportService;

public class MainActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CryptoManager cryptoManager = App.getAppContainer().getCryptoManager();

        Intent intent;

        if (cryptoManager.isFirstRun()) {
            intent = new Intent(this, StartActivity.class);
        } else if (cryptoManager.isBlocked()) {
            intent = new Intent(this, KeyRecoveryActivity.class);
        } else if (App.getContext().serviceRunning(ExportService.class)) {
            intent = new Intent(this, ExportActivity.class);
        } else if (!cryptoManager.ready()) {
            intent = new Intent(this, AuthActivity.class);
            intent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);
        } else {
            intent = new Intent(this, NotesListActivity.class);
        }

        startActivity(intent);
        finish();
    }
}