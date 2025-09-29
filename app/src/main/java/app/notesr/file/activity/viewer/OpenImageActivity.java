package app.notesr.file.activity.viewer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import app.notesr.R;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.service.FileService;
import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtils;

public class OpenImageActivity extends MediaFileViewerActivityBase {

    private ImageView imageView;
    private TextView errorMessageTextView;
    private FileService fileService;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_image);

        saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        imageView = findViewById(R.id.imageView);
        errorMessageTextView = findViewById(R.id.errorMessageTextView);
        fileService = getFileService();

        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        imageView.setOnTouchListener(new ZoomableImageTouchListener(imageView));

        loadImage();
    }

    private void loadImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AlertDialogTheme);

        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);
        AlertDialog progressDialog = builder.create();

        newSingleThreadExecutor().execute(() -> {
            runOnUiThread(progressDialog::show);

            try {
                if (!isFileSizeAllowed(fileInfo.getSize())) {
                    runOnUiThread(() ->
                            errorMessageTextView.setText(R.string.failed_to_load_the_image));
                    return;
                }

                byte[] imageBytes = fileService.read(fileInfo.getId());
                Bitmap bitmap = decodeAndScaleBitmap(imageBytes);
                bitmap = applyExifOrientation(imageBytes, bitmap);

                Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> imageView.setImageBitmap(finalBitmap));

            } catch (DecryptionFailedException | IOException | OutOfMemoryError e) {
                Log.e("OpenImageActivity", "Error loading image", e);
                runOnUiThread(() ->
                        errorMessageTextView.setText(R.string.failed_to_load_the_image));
            } finally {
                runOnUiThread(progressDialog::dismiss);
            }
        });
    }

    private boolean isFileSizeAllowed(long fileSize) {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freeMemory = Runtime.getRuntime().maxMemory() - usedMemory;

        long maxAllowedSize = freeMemory / 4;

        if (fileSize > maxAllowedSize) {
            Log.w("OpenImageActivity", "File too large: " + fileSize
                    + " bytes, limit: " + maxAllowedSize);

            return false;
        }

        return true;
    }

    private Bitmap decodeAndScaleBitmap(byte[] imageBytes) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        options.inSampleSize = calculateInSampleSize(options, screenWidth, screenHeight);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private Bitmap applyExifOrientation(byte[] imageBytes, Bitmap bitmap) throws IOException {
        int orientation;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            ExifInterface exif = new ExifInterface(inputStream);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        }

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
        }

        if (!matrix.isIdentity()) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
        }

        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {

                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    private FileService getFileService() {
        Context context = getApplicationContext();

        AppDatabase db = DatabaseProvider.getInstance(this);
        CryptoSecrets secrets = CryptoManagerProvider.getInstance().getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));
        FilesUtils filesUtils = new FilesUtils();

        return new FileService(context, db, cryptor, filesUtils);
    }
}