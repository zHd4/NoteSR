package app.notesr.note.activity;

import android.content.Context;
import android.content.Intent;

import app.notesr.security.activity.AuthActivity;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;

import java.util.function.Consumer;

public final class LockOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        Context context = activity.getApplicationContext();
        Intent authActivityIntent = new Intent(context, AuthActivity.class);

        authActivityIntent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        cryptoManager.destroySecrets();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
