/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class FileExifDataResolver {

    private final Context context;
    private final FilesUtilsAdapter filesUtils;
    private final Uri fileUri;

    public String getFileName() {
        try (Cursor cursor = getCursor(fileUri)) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            cursor.moveToFirst();
            return cursor.getString(index);
        }
    }

    public long getFileSize() {
        try (Cursor cursor = getCursor(fileUri)) {
            int index = cursor.getColumnIndex(OpenableColumns.SIZE);

            cursor.moveToFirst();
            return cursor.getLong(index);
        }
    }

    public String getMimeType() {
        String type = null;
        String extension = filesUtils.getFileExtension(getFileName());

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private Cursor getCursor(Uri uri) {
        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor == null) {
            throw new NullPointerException("Cursor is null");
        }

        return cursor;
    }
}
