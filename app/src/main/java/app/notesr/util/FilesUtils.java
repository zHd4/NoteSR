package app.notesr.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FilesUtils implements FilesUtilsAdapter {
    /** @noinspection ResultOfMethodCallIgnored*/
    public byte[] readFileBytes(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];

        try (FileInputStream stream = new FileInputStream(file)) {
            stream.read(data);
        }

        return data;
    }

    public void writeFileBytes(File file, byte[] data) throws IOException {
        writeFileBytes(file, data, false);
    }

    public void writeFileBytes(File file, byte[] data, boolean append) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file, append)) {
            stream.write(data);
        }
    }

    public File getInternalFile(Context context, String path) {
        return new File(context.getFilesDir(), path);
    }

    public File getDatabaseFile(Context context, String filename) {
        return new File(context.getDatabasePath(filename).getPath());
    }

    public String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex != -1) ? fileName.substring(lastIndex + 1) : null;
    }
}
