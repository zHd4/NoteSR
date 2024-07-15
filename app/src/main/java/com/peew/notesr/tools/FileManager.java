package com.peew.notesr.tools;

import com.peew.notesr.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
        long fileSize = file.length();

        try (FileOutputStream stream = new FileOutputStream(file)) {
            try {
                stream.write(new byte[(int) fileSize]);
            } catch (OutOfMemoryError e) {
                long bytesWrite = 0;

                do {
                    byte[] empty = new byte[(int) (getAvailableMemory() / 2)];

                    stream.write(empty);
                    bytesWrite += empty.length;
                } while (bytesWrite < fileSize);
            }
        }

        return file.delete();
    }

    public static String getFileExtension(String filename) {
        String extension = null;
        String[] array = filename.split("\\.");

        if (array.length >= 2) {
            return new LinkedList<>(Arrays.asList(array)).getLast();
        }

        return null;
    }

    private static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }
}
