package app.notesr.onclick.security;

import android.content.Intent;

import app.notesr.App;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.activity.security.AuthActivity;

import java.util.function.Consumer;

public class LockOnClick implements Consumer<NoteListActivity> {
    @Override
    public void accept(NoteListActivity activity) {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

        App.getAppContainer().getCryptoManager().destroyKey();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
