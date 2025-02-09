package app.notesr.onclick.security;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.activity.security.SetupKeyActivity;

import java.util.function.Consumer;

public class GenerateNewKeyOnClick implements Consumer<NoteListActivity> {
    @Override
    public void accept(NoteListActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_re_encryption_warning);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.yes, getRegenerateKeyDialogOnClick(activity));
        builder.setNegativeButton(R.string.no, getRegenerateKeyDialogOnClick(activity));

        builder.create().show();
    }

    private DialogInterface.OnClickListener getRegenerateKeyDialogOnClick(NoteListActivity activity) {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);

                String password = App.getAppContainer()
                        .getCryptoManager()
                        .getCryptoKeyInstance()
                        .getPassword();

                setupKeyActivityIntent.putExtra("mode",
                        SetupKeyActivity.Mode.REGENERATION.toString());

                setupKeyActivityIntent.putExtra("password", password);

                activity.startActivity(setupKeyActivityIntent);
            }
        };
    }
}
