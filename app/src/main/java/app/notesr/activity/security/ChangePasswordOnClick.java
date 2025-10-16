package app.notesr.activity.security;

import android.content.Intent;

import app.notesr.activity.ActivityBase;

import java.util.function.Consumer;

public final class ChangePasswordOnClick implements Consumer<ActivityBase> {
    @Override
    public void accept(ActivityBase activity) {
        Intent authActivityIntent = new Intent(activity.getApplicationContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CHANGE_PASSWORD.toString());

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
