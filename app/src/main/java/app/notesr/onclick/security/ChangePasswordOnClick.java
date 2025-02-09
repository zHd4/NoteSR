package app.notesr.onclick.security;

import android.content.Intent;

import app.notesr.App;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.activity.security.AuthActivity;

import java.util.function.Consumer;

public class ChangePasswordOnClick implements Consumer<NoteListActivity> {
    @Override
    public void accept(NoteListActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CHANGE_PASSWORD.toString());

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
