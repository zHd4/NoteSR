package app.notesr.note.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import app.notesr.R;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.security.activity.KeySetupMode;
import app.notesr.security.activity.SetupKeyActivity;

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
                String password = cryptoManager.getSecrets().getPassword();

                intent.putExtra("mode", KeySetupMode.REGENERATION.toString());
                intent.putExtra("password", password);

                activity.startActivity(intent);
            }
        };
    }
}
