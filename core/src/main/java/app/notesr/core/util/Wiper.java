/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public final class Wiper implements WiperAdapter {

    private static final String TAG = Wiper.class.getCanonicalName();
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int LOOPS_COUNT = 6;

    public void wipeDir(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException("Directory is null or not a directory");
        }

        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Cannot list directory " + dir.getAbsolutePath());
        }

        Log.d(TAG, "Wiping directory " + dir.getAbsolutePath());

        for (File file : files) {
            if (file.isDirectory()) {
                wipeDir(file);
            } else {
                wipeFile(file);
            }
        }

        Files.delete(dir.toPath());
    }

    public void wipeFile(File file) throws IOException {
        Log.d(TAG, "Wiping file " + file.getAbsolutePath());

        for (int i = 0; i < LOOPS_COUNT; i++) {
            wipeFileData(file);
        }

        Files.delete(file.toPath());
    }

    private static void wipeFileData(File file) throws IOException {
        long fileSize = file.length();

        if (fileSize <= 0) {
            Log.d(TAG, "File " + file.getAbsolutePath() + "  is empty");
            return;
        }

        Log.d(TAG, "Size of " + file.getAbsolutePath() + " is " + fileSize);

        try (FileOutputStream stream = new FileOutputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(fileSize, BUFFER_SIZE)];
            long bytesWritten = 0;

            Log.d(TAG, "Buffer size for " + file.getAbsolutePath() + " is " + buffer.length
                    + ", bytes written: " + bytesWritten);


            while (bytesWritten < fileSize) {
                int toWrite = (int) Math.min(buffer.length, fileSize - bytesWritten);
                stream.write(buffer, 0, toWrite);
                bytesWritten += toWrite;
            }

            stream.getFD().sync();
        }
    }
}
