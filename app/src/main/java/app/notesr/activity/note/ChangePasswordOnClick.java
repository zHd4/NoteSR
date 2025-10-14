package app.notesr.activity.note;

import android.content.Intent;

import app.notesr.activity.security.AuthActivity;

import java.util.function.Consumer;

public final class ChangePasswordOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        Intent authActivityIntent = new Intent(activity.getApplicationContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CHANGE_PASSWORD.toString());

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
