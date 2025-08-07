package app.notesr.note;

import android.content.Intent;

import app.notesr.crypto.AuthActivity;
import app.notesr.crypto.CryptoManager;

import java.util.function.Consumer;

public class LockOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        Intent authActivityIntent = new Intent(activity.getApplicationContext(),
                AuthActivity.class);

        authActivityIntent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

        CryptoManager cryptoManager = CryptoManager.getInstance();
        cryptoManager.destroySecrets();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
