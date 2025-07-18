package app.notesr.file.viewer;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.notesr.App;
import app.notesr.R;
import app.notesr.db.service.dao.TempFileDao;
import app.notesr.model.TempFile;
import app.notesr.service.android.CacheCleanerAndroidService;

public class OpenImageActivity extends MediaFileViewerActivityBase {
    private static final int MAX_IMAGE_SIZE = 4096;

    private ScaleGestureDetector scaleGestureDetector;
    private ImageView imageView;
    private TextView errorMessageTextView;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        imageView = findViewById(R.id.assignedImageView);
        errorMessageTextView = findViewById(R.id.errorMessageTextView);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(imageView));

        loadImage();
        startForegroundService(new Intent(getApplicationContext(), CacheCleanerAndroidService.class));
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

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);
            imageFile = dropToCache(fileInfo);

            if (!isThumbnailSet()) {
                setThumbnail(imageFile);
            }

            runOnUiThread(progressDialog::dismiss);
            runOnUiThread(() -> {
                Uri imageUri = Uri.parse(imageFile.getAbsolutePath());

                TempFileDao tempFileDao = App.getAppContainer()
                        .getServicesDB()
                        .getDao(TempFileDao.class);

                TempFile tempImageFile = new TempFile(imageUri);

                tempFileDao.save(tempImageFile);

                if (!isImageTooLarge(imageFile)) {
                    imageView.setImageURI(imageUri);
                } else {
                    errorMessageTextView.setText(getString(R.string.image_is_too_large_to_open));
                }
            });
        });
    }

    private boolean isImageTooLarge(File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        int width = options.outWidth;
        int height = options.outHeight;

        return width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE;
    }
}