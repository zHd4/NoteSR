package com.peew.notesr.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager {
    /** @noinspection ResultOfMethodCallIgnored*/
    public static byte[] readFileBytes(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);

        byte[] data = new byte[(int) file.length()];
        stream.read(data);

        stream.close();
        return data;
    }

    public static void writeFileBytes(File file, byte[] data) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);

        stream.write(data);
        stream.close();
    }
}
