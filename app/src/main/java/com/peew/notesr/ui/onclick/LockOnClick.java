package com.peew.notesr.ui.onclick;

import android.content.Intent;

import com.peew.notesr.App;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.auth.AuthActivity;

import java.util.function.Consumer;

public class LockOnClick implements Consumer<MainActivity> {
    private static final CryptoManager cryptoManager = CryptoManager.getInstance();
    @Override
    public void accept(MainActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

        cryptoManager.destroyKey();
        activity.startActivity(authActivityIntent);

        activity.finish();
    }
}
