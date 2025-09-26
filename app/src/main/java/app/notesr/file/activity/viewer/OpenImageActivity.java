package app.notesr.file.activity.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.IOException;

import app.notesr.R;
import app.notesr.db.DatabaseProvider;
import app.notesr.cleaner.model.TempFile;
import app.notesr.cleaner.service.CacheCleanerAndroidService;
import app.notesr.cleaner.service.TempFileService;

public class OpenImageActivity extends MediaFileViewerActivityBase {

    private TempFileService tempFileService;
    private CustomImageView imageView;
    private TextView errorMessageTextView;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        Context context = getApplicationContext();
        tempFileService = new TempFileService(DatabaseProvider.getInstance(context));

        imageView = findViewById(R.id.assignedImageView);
        errorMessageTextView = findViewById(R.id.errorMessageTextView);

        loadImage();
        startForegroundService(new Intent(getApplicationContext(), CacheCleanerAndroidService.class));
    }

    private void loadImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);

            imageFile = dropToCache();

            if (!isThumbnailSet()) {
                setThumbnail(imageFile);
            }

            Uri imageUri = Uri.parse(imageFile.getAbsolutePath());

            TempFile tempImageFile = new TempFile();
            tempImageFile.setUri(imageUri);

            tempFileService.save(tempImageFile);

            runOnUiThread(() -> {
                progressDialog.dismiss();

                try {
                    imageView.setImage(imageFile);
                } catch (IOException e) {
                    Log.e("OpenImageActivity", "Error loading image", e);
                    errorMessageTextView.setText(R.string.failed_to_load_the_image);
                }
            });
        });
    }
}