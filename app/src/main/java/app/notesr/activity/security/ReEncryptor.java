package app.notesr.activity.security;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.dto.CryptoKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ReEncryptor implements Runnable {

    private final Activity activity;
    private final CryptoKey cryptoKey;

    @Override
    public void run() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        executor.execute(() -> {
            handler.post(() -> {
                builder.setView(R.layout.progress_dialog_re_encryption);
                builder.setCancelable(false);
                builder.create().show();
            });

            try {
                App.getAppContainer().getKeyUpdateService().updateEncryptedData(cryptoKey);

                activity.startActivity(new Intent(activity, NotesListActivity.class));
                activity.finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
