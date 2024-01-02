package com.peew.notesr.ui.onclick;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.setup.SetupKeyActivity;

import java.util.function.Consumer;

public class GenerateNewKeyOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_re_encryption_warning);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.yes, getRegenerateKeyDialogOnClick(activity));
        builder.setNegativeButton(R.string.no, getRegenerateKeyDialogOnClick(activity));

        builder.create().show();
    }

    private DialogInterface.OnClickListener getRegenerateKeyDialogOnClick(MainActivity activity) {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);
                String password = CryptoManager.getInstance().getCryptoKeyInstance().password();

                setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.REGENERATION_MODE);
                setupKeyActivityIntent.putExtra("password", password);

                activity.startActivity(setupKeyActivityIntent);
                activity.finish();
            }
        };
    }
}
