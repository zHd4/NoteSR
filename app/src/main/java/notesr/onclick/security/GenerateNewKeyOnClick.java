package notesr.onclick.security;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import notesr.App;
import notesr.R;
import notesr.activity.notes.NotesListActivity;
import notesr.activity.security.SetupKeyActivity;

import java.util.function.Consumer;

public class GenerateNewKeyOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_re_encryption_warning);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.yes, getRegenerateKeyDialogOnClick(activity));
        builder.setNegativeButton(R.string.no, getRegenerateKeyDialogOnClick(activity));

        builder.create().show();
    }

    private DialogInterface.OnClickListener getRegenerateKeyDialogOnClick(NotesListActivity activity) {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);

                String password = App.getAppContainer()
                        .getCryptoManager()
                        .getCryptoKeyInstance()
                        .getPassword();

                setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.REGENERATION_MODE);
                setupKeyActivityIntent.putExtra("password", password);

                activity.startActivity(setupKeyActivityIntent);
                activity.finish();
            }
        };
    }
}
