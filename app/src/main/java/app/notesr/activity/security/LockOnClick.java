package app.notesr.activity.security;

import android.content.Context;
import android.content.Intent;

import app.notesr.activity.note.NotesListActivity;
import app.notesr.core.security.SecretCache;
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

        SecretCache.clear();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
