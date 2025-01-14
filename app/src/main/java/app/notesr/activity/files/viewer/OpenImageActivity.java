package app.notesr.activity.files.viewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.notesr.App;
import app.notesr.R;
import app.notesr.db.services.table.TempFilesTable;
import app.notesr.model.TempFile;
import app.notesr.service.android.CacheCleanerService;

public class OpenImageActivity extends MediaFileViewerActivityBase {
    private ScaleGestureDetector scaleGestureDetector;
    private ImageView imageView;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        imageView = findViewById(R.id.assignedImageView);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(imageView));

        loadImage();
        startForegroundService(new Intent(getApplicationContext(), CacheCleanerService.class));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void loadImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        TempFilesTable tempFilesTable = App.getAppContainer()
                .getServicesDB()
                .getTable(TempFilesTable.class);

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);
            imageFile = dropToCache(fileInfo);

            if (!isThumbnailSet()) {
                setThumbnail(imageFile);
            }

            runOnUiThread(progressDialog::dismiss);
            runOnUiThread(() -> {
                Uri imageUri = Uri.parse(imageFile.getAbsolutePath());
                TempFile imageFile = new TempFile(imageUri);

                tempFilesTable.save(imageFile);
                imageView.setImageURI(imageUri);
            });
        });
    }
}