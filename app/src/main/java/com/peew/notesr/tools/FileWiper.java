package com.peew.notesr.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWiper {

    private static final int LOOPS_COUNT = 6;

    private final File file;

    public FileWiper(File file) {
        this.file = file;
    }

    public boolean wipeFile() throws IOException {
        long fileSize = file.length();

        for (int i = 0; i < LOOPS_COUNT; i++) {
            wipeFileData(fileSize);
        }

        return file.delete();
    }

    private void wipeFileData(long size) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file)) {
            try {
                stream.write(new byte[(int) size]);
            } catch (OutOfMemoryError e) {
                long bytesWrite = 0;

                do {
                    byte[] empty = new byte[(int) (getAvailableMemory() / 2)];

                    stream.write(empty);
                    bytesWrite += empty.length;
                } while (bytesWrite < size);
            }
        }
    }

    private long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }
}
