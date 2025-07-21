package app.notesr.note;

import android.content.Intent;

import app.notesr.crypto.AuthActivity;

import java.util.function.Consumer;

public class ChangePasswordOnClick implements Consumer<NoteListActivity> {
    @Override
    public void accept(NoteListActivity activity) {
        Intent authActivityIntent = new Intent(activity.getApplicationContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CHANGE_PASSWORD.toString());

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
