package com.peew.notesr.onclick.security;

import android.content.Intent;

import com.peew.notesr.App;
import com.peew.notesr.activity.MainActivity;
import com.peew.notesr.activity.AuthActivity;

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
