package com.peew.notesr.ui.onclick;

import android.content.Intent;

import com.peew.notesr.App;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.auth.AuthActivity;

import java.util.function.Consumer;

public class ChangePasswordOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.CHANGE_PASSWORD_MODE);

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
