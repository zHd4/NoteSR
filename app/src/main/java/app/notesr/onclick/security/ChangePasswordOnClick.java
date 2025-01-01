package app.notesr.onclick.security;

import android.content.Intent;

import app.notesr.App;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.activity.security.AuthActivity;

import java.util.function.Consumer;

public class ChangePasswordOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CHANGE_PASSWORD.toString());

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
