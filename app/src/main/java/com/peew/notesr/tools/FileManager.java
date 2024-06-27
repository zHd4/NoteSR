package com.peew.notesr.tools;

import com.peew.notesr.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager {
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

    /** @noinspection UnusedReturnValue*/
    public static boolean wipeFile(File file) throws IOException {
        int size = (int) file.length();

        try (FileOutputStream stream = new FileOutputStream(file)) {
            for (int i = 0; i < size; i++) {
                stream.write(0);
            }
        }

        return file.delete();
    }
}
