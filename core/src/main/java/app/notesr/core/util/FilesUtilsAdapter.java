/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public interface FilesUtilsAdapter {
    File getInternalFile(Context context, String name);
    File getDatabaseFile(Context context, String path);
    byte[] readFileBytes(File file) throws IOException;
    void writeFileBytes(File file, byte[] data) throws IOException;
    void writeFileBytes(File file, byte[] data, boolean append) throws IOException;
    String getFileExtension(String fileName);
}
