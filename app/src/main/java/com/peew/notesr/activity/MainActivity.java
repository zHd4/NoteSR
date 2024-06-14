package com.peew.notesr.activity;

import android.content.Intent;
import android.os.Bundle;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.activity.security.AuthActivity;
import com.peew.notesr.activity.security.KeyRecoveryActivity;
import com.peew.notesr.crypto.CryptoManager;

public class MainActivity extends AppCompatActivityExtended {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CryptoManager cryptoManager = App.getAppContainer().getCryptoManager();

        if (cryptoManager.isFirstRun()) {
            startActivity(new Intent(App.getContext(), StartActivity.class));
        } else if (cryptoManager.isBlocked()) {
            startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
        } else if (!cryptoManager.ready()) {
            Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
            authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

            startActivity(authActivityIntent);
        } else {
            startActivity(new Intent(App.getContext(), NotesListActivity.class));
        }

        finish();
    }
}