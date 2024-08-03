package com.peew.notesr.tools;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import com.peew.notesr.App;

public class FileExifDataResolver {

    private final Uri uri;

    public FileExifDataResolver(Uri uri) {
        this.uri = uri;
    }

    public String getFileName() {
        try (Cursor cursor = getCursor(uri)) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            cursor.moveToFirst();
            return cursor.getString(index);
        }
    }

    public long getFileSize() {
        try (Cursor cursor = getCursor(uri)) {
            int index = cursor.getColumnIndex(OpenableColumns.SIZE);

            cursor.moveToFirst();
            return cursor.getLong(index);
        }
    }

    public String getMimeType() {
        return getMimeType(getFileName());
    }

    private Cursor getCursor(Uri uri) {
        Cursor cursor = App.getContext()
                .getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor == null) {
            throw new RuntimeException(new NullPointerException("Cursor is null"));
        }

        return cursor;
    }

    private static String getMimeType(String filename) {
        String type = null;
        String extension = FileManager.getFileExtension(filename);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }
}
