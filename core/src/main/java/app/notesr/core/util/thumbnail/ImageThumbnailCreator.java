/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util.thumbnail;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import app.notesr.core.util.FileExifDataResolver;
import app.notesr.core.util.FilesUtilsAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImageThumbnailCreator implements ThumbnailCreator {

    private static final String TAG = ImageThumbnailCreator.class.getCanonicalName();

    public static final int WIDTH = 100;
    public static final int HEIGHT = 100;
    public static final int QUALITY = 80;

    private final Context context;
    private final FilesUtilsAdapter filesUtils;

    private static final Map<String, Bitmap.CompressFormat> COMPRESS_FORMAT_MAP = Map.of(
            "jpg", Bitmap.CompressFormat.JPEG,
            "jpeg", Bitmap.CompressFormat.JPEG,
            "png", Bitmap.CompressFormat.PNG,
            "webp", Bitmap.CompressFormat.WEBP,
            "bmp", Bitmap.CompressFormat.PNG
    );

    @Override
    public byte[] getThumbnail(Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (InputStream inputStream = getInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Could not open input stream for URI: " + uri);
            }

            BitmapFactory.decodeStream(inputStream, null, options);
        }

        int scaleFactor = Math.min(options.outWidth / WIDTH, options.outHeight / HEIGHT);

        options.inJustDecodeBounds = false;
        options.inSampleSize = Math.max(1, scaleFactor);

        Bitmap bitmap;

        try (InputStream inputStream = getInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Could not open input stream for URI: " + uri);
            }

            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        }

        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI: " + uri);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            bitmap.compress(getImageCompressFormat(uri), QUALITY, outputStream);
            return outputStream.toByteArray();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unsupported image format", e);
            throw new UnsupportedOperationException("Unsupported image format", e);
        } finally {
            bitmap.recycle();
        }
    }

    private InputStream getInputStream(Uri uri) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(uri);
    }

    private Bitmap.CompressFormat getImageCompressFormat(Uri uri) {
        String filename = "file".equalsIgnoreCase(uri.getScheme())
                ? new File(requireNonNull(uri.getPath())).getName()
                : new FileExifDataResolver(context, filesUtils, uri).getFileName();

        String extension = filesUtils.getFileExtension(filename);

        if (extension == null) {
            throw new IllegalArgumentException("Unsupported image format: extension is null");
        }

        String lowerExtension = extension.toLowerCase(Locale.ROOT);

        if (!COMPRESS_FORMAT_MAP.containsKey(lowerExtension)) {
            throw new IllegalArgumentException("Unsupported image format: " + extension);
        }

        return COMPRESS_FORMAT_MAP.get(lowerExtension);
    }
}
