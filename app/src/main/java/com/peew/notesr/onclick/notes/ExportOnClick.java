package com.peew.notesr.onclick.notes;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.R;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.manager.export.ExportManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ExportOnClick implements Consumer<NotesListActivity> {
    @Override
    public void accept(NotesListActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_exporting).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handler.post(progressDialog::show);

            try {
                File tempFile = File.createTempFile("bak", ".json");
                String path = tempFile.getPath();

                ExportManager exportManager = new ExportManager(activity.getApplicationContext());
                exportManager.export(path);

                String message = String.format(activity.getString(R.string.saved_to), path);

                activity.runOnUiThread(() -> activity.showToastMessage(message, Toast.LENGTH_SHORT));
//            } catch (MissingNotesException e) {
//                String message = activity.getString(R.string.no_notes);
//                activity.runOnUiThread(() -> activity.showToastMessage(message, Toast.LENGTH_SHORT));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            progressDialog.dismiss();
        });
    }
}
