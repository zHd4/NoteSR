package com.peew.notesr.onclick.notes;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.R;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.manager.export.ExportManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = getOutputFile(outputDir.getPath());

                ExportManager exportManager = new ExportManager(activity.getApplicationContext());
                exportManager.export(outputFile);

                String message = String.format(activity.getString(R.string.saved_to), outputFile.getAbsolutePath());

                activity.runOnUiThread(() -> activity.showToastMessage(message, Toast.LENGTH_LONG));
//            } catch (MissingNotesException e) {
//                String message = activity.getString(R.string.no_notes);
//                activity.runOnUiThread(() -> activity.showToastMessage(message, Toast.LENGTH_SHORT));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            progressDialog.dismiss();
        });
    }

    private File getOutputFile(String dirPath) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String filename = "nsr_export_" + nowStr + ".notesr.bak";
        Path outputPath = Paths.get(dirPath, filename);

        return new File(outputPath.toUri());
    }
}
