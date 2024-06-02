package com.peew.notesr.ui.onclick.security;

import android.content.Intent;

import com.peew.notesr.App;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.auth.AuthActivity;

import java.util.function.Consumer;

public class LockOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

        App.getAppContainer().getCryptoManager().destroyKey();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
