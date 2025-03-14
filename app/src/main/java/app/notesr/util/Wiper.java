package app.notesr.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Wiper {

    private static final String TAG = Wiper.class.getName();
    private static final int LOOPS_COUNT = 6;

    public static boolean wipeDir(File dir) throws IOException {
        for (File file : listDirFiles(dir)) {
            if (file.isDirectory()) {
                wipeDir(file);
                file.delete();
            } else {
                wipeFile(file);
            }
        }

        return dir.delete();
    }

    public static boolean wipeFile(File file) throws IOException {
        for (int i = 0; i < LOOPS_COUNT; i++) {
            wipeFileData(file);
        }

        return file.delete();
    }

    private static void wipeFileData(File file) throws IOException {
        long fileSize = file.length();

        try (FileOutputStream stream = new FileOutputStream(file)) {
            try {
                stream.write(new byte[(int) fileSize]);
            } catch (OutOfMemoryError e) {
                long bytesWrite = 0;

                do {
                    try {
                        byte[] empty = new byte[(int) (getAvailableMemory() / 2)];

                        stream.write(empty);
                        bytesWrite += empty.length;
                    } catch (OutOfMemoryError error) {
                        Log.e(TAG, "OutOfMemoryError", error);
                    }

                } while (bytesWrite < fileSize);
            }
        }
    }

    private static List<File> listDirFiles(File dir) {
        return Arrays.asList(Objects.requireNonNull(dir.listFiles()));
    }

    private static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }
}
