package app.notesr.note.activity;

import android.content.Intent;

import app.notesr.security.activity.AuthActivity;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;

import java.util.function.Consumer;

public class LockOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        Intent authActivityIntent = new Intent(activity.getApplicationContext(),
                AuthActivity.class);

        authActivityIntent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
        cryptoManager.destroySecrets();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
