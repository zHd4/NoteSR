/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/**
 * Interface providing an abstraction for file system operations.
 * Decouples the application logic from direct Android file API calls to facilitate
 * testing and portability.
 */
public interface FilesUtilsAdapter {

    /**
     * Retrieves a file object pointing to the internal storage directory of the application.
     *
     * @param context The application context.
     * @param name    The name of the file.
     * @return A {@link File} object representing the path in internal storage.
     */
    File getInternalFile(Context context, String name);

    /**
     * Retrieves a file object pointing to a database file.
     *
     * @param context The application context.
     * @param path    The name or relative path of the database.
     * @return A {@link File} object representing the database path.
     */
    File getDatabaseFile(Context context, String path);

    /**
     * Reads the entire contents of a file into a byte array.
     *
     * @param file The file to read.
     * @return A byte array containing the file's data.
     * @throws IOException If an I/O error occurs during reading.
     */
    byte[] readFileBytes(File file) throws IOException;

    /**
     * Writes a byte array to a file, overwriting any existing content.
     *
     * @param file The file to write to.
     * @param data The byte array to write.
     * @throws IOException If an I/O error occurs during writing.
     */
    void writeFileBytes(File file, byte[] data) throws IOException;

    /**
     * Writes a byte array to a file, with an option to append or overwrite.
     *
     * @param file   The file to write to.
     * @param data   The byte array to write.
     * @param append True to append data to the end of the file, false to overwrite.
     * @throws IOException If an I/O error occurs during writing.
     */
    void writeFileBytes(File file, byte[] data, boolean append) throws IOException;

    /**
     * Moves a file from a source location to a target location.
     *
     * @param source The source file to move.
     * @param target The destination file.
     * @throws IOException If the move operation fails.
     */
    void moveFile(File source, File target) throws IOException;

    /**
     * Deletes a file from the file system.
     *
     * @param file The file to be deleted.
     * @throws IOException If the file cannot be deleted.
     */
    void deleteFile(File file) throws IOException;

    /**
     * Extracts the file extension from a given file name.
     *
     * @param fileName The name of the file including its extension.
     * @return The extension string (e.g., "txt"), or an empty string if no extension is found.
     */
    String getFileExtension(String fileName);
}
