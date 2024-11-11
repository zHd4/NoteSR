package app.notesr.utils;

import app.notesr.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class FilesUtils {
    /** @noinspection ResultOfMethodCallIgnored*/
    public static byte[] readFileBytes(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];

        try (FileInputStream stream = new FileInputStream(file)) {
            stream.read(data);
        }

        return data;
    }

    public static void writeFileBytes(File file, byte[] data) throws IOException {
        writeFileBytes(file, data, false);
    }

    public static void writeFileBytes(File file, byte[] data, boolean append) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file, append)) {
            stream.write(data);
        }
    }

    public static File getInternalFile(String path) {
        return new File(App.getContext().getFilesDir(), path);
    }

    public static String getFileExtension(String filename) {
        String[] array = filename.split("\\.");

        if (array.length >= 2) {
            return new LinkedList<>(Arrays.asList(array)).getLast();
        }

        return null;
    }
}
