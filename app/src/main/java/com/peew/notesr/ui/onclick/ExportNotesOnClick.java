package com.peew.notesr.ui.onclick;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.peew.notesr.R;
import com.peew.notesr.db.notes.NotesExporter;
import com.peew.notesr.ui.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ExportNotesOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_exporting).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handler.post(progressDialog::show);

            try {
                String path = new NotesExporter(activity).export();
                String message = String.format(activity.getString(R.string.saved_to), path);

                activity.runOnUiThread(() -> activity.showToastMessage(message, Toast.LENGTH_SHORT));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            progressDialog.dismiss();
        });
    }
}
