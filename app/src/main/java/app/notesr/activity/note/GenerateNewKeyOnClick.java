package app.notesr.activity.note;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import app.notesr.R;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.activity.security.KeySetupMode;
import app.notesr.activity.security.SetupKeyActivity;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class GenerateNewKeyOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_re_encryption_warning);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.yes, regenerateKeyDialogOnClick(activity));
        builder.setNegativeButton(R.string.no, regenerateKeyDialogOnClick(activity));

        builder.create().show();
    }

    private DialogInterface.OnClickListener regenerateKeyDialogOnClick(NotesListActivity activity) {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Context context = activity.getApplicationContext();
                Intent intent = new Intent(context, SetupKeyActivity.class);

                CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
                char[] password = cryptoManager.getSecrets().getPassword();

                intent.putExtra("mode", KeySetupMode.REGENERATION.toString());

                try {
                    SecretCache.put("password", charsToBytes(password, StandardCharsets.UTF_8));
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                }

                activity.startActivity(intent);
            }
        };
    }
}
