/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util.thumbnail;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VideoThumbnailCreator implements ThumbnailCreator {
    private static final Bitmap.CompressFormat THUMBNAIL_FORMAT = Bitmap.CompressFormat.WEBP;
    private static final int TIME_US = 1;
    private static final int QUALITY = 90;
    private static final int WIDTH = 200;

    private final Context context;

    @Override
    public byte[] getThumbnail(Uri uri) throws IOException {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(context, uri);
            Bitmap original = retriever.getFrameAtTime(TIME_US);

            if (original == null) {
                return null;
            }

            int width = original.getWidth();
            int height = original.getHeight();

            if (width > WIDTH) {
                float scale = (float) WIDTH / width;
                int newHeight = Math.round(height * scale);

                Bitmap scaled = Bitmap.createScaledBitmap(original, WIDTH, newHeight, true);

                original.recycle();
                original = scaled;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            original.compress(THUMBNAIL_FORMAT, QUALITY, stream);
            retriever.release();

            return stream.toByteArray();
        }
    }
}
