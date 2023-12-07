package com.peew.notesr.activities;

import android.content.Intent;
import android.os.Bundle;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;

public class MainActivity extends ExtendedAppCompatActivity {
    private final CryptoManager cryptoManager = CryptoManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(cryptoManager.isFirstRun()) {
            startActivity(new Intent(App.getContext(), StartActivity.class));
        } else if (!cryptoManager.ready()) {
            Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
            authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

            startActivity(authActivityIntent);
        }

        if (cryptoManager.isBlocked()) {
            startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
        }
    }
}