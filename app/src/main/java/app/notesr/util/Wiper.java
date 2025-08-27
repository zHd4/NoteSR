package app.notesr.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class Wiper implements WiperAdapter {
    private static final String TAG = Wiper.class.getName();
    private static final int LOOPS_COUNT = 6;

    public void wipeDir(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException("Directory is null or not a directory");
        }

        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Cannot list directory " + dir.getAbsolutePath());
        }

        for (File file : files) {
            if (file.isDirectory()) {
                wipeDir(file);
                wipeFile(file);
            } else {
                wipeFile(file);
            }
        }

        Files.delete(dir.toPath());
    }

    public void wipeFile(File file) throws IOException {
        for (int i = 0; i < LOOPS_COUNT; i++) {
            wipeFileData(file);
        }

        Files.delete(file.toPath());
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
                        Log.e(TAG, "Failed to allocate memory for wipe", error);
                    }

                } while (bytesWrite < fileSize);
            }
        }
    }

    private static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }
}
