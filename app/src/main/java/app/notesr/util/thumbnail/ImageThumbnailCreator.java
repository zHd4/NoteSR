package app.notesr.util.thumbnail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;

import app.notesr.util.FilesUtils;

public class ImageThumbnailCreator implements ThumbnailCreator {
    public static final int WIDTH = 100;
    public static final int HEIGHT = 100;
    public static final int QUALITY = 80;

    private static final Map<String, Bitmap.CompressFormat> COMPRESS_FORMAT_MAP = Map.of(
            "jpg", Bitmap.CompressFormat.JPEG,
            "jpeg", Bitmap.CompressFormat.JPEG,
            "png", Bitmap.CompressFormat.PNG,
            "webp", Bitmap.CompressFormat.WEBP
    );

    @Override
    public byte[] getThumbnail(File file) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getInputStream(file), null, options);

        int scaleFactor = Math.min(options.outWidth / WIDTH, options.outHeight / HEIGHT);

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeStream(getInputStream(file), null, options);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Objects.requireNonNull(bitmap)
                .compress(getImageCompressFormat(file), QUALITY, outputStream);

        return outputStream.toByteArray();
    }

    private FileInputStream getInputStream(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    private Bitmap.CompressFormat getImageCompressFormat(File file) {
        String extension = new FilesUtils().getFileExtension(file.getName());

        if (!COMPRESS_FORMAT_MAP.containsKey(extension)) {
            throw new IllegalArgumentException("Unsupported image format");
        }

        return COMPRESS_FORMAT_MAP.get(extension);
    }
}
