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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import app.notesr.core.util.FileExifDataResolver;
import app.notesr.core.util.FilesUtilsAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImageThumbnailCreator implements ThumbnailCreator {
    public static final int WIDTH = 100;
    public static final int HEIGHT = 100;
    public static final int QUALITY = 80;

    private final Context context;
    private final FilesUtilsAdapter filesUtils;

    private static final Map<String, Bitmap.CompressFormat> COMPRESS_FORMAT_MAP = Map.of(
            "jpg", Bitmap.CompressFormat.JPEG,
            "jpeg", Bitmap.CompressFormat.JPEG,
            "png", Bitmap.CompressFormat.PNG,
            "webp", Bitmap.CompressFormat.WEBP
    );

    @Override
    public byte[] getThumbnail(Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getInputStream(uri), null, options);

        int scaleFactor = Math.min(options.outWidth / WIDTH, options.outHeight / HEIGHT);

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeStream(getInputStream(uri), null, options);
        requireNonNull(bitmap, "Bitmap is null");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(getImageCompressFormat(uri), QUALITY, outputStream);

        return outputStream.toByteArray();
    }

    private InputStream getInputStream(Uri uri) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(uri);
    }

    private Bitmap.CompressFormat getImageCompressFormat(Uri uri) {
        String filename = "file".equalsIgnoreCase(uri.getScheme())
                ? new File(requireNonNull(uri.getPath())).getName()
                : new FileExifDataResolver(context, filesUtils, uri).getFileName();

        String extension = filesUtils.getFileExtension(filename);

        if (!COMPRESS_FORMAT_MAP.containsKey(extension)) {
            throw new IllegalArgumentException("Unsupported image format");
        }

        return COMPRESS_FORMAT_MAP.get(extension);
    }
}
