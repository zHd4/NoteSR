/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    public static boolean isZipArchive(String path) throws IOException {
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            return false;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] signature = new byte[4];

            if (fileInputStream.read(signature) == 4) {
                return (signature[0] == 0x50
                        && signature[1] == 0x4B
                        && signature[2] == 0x03
                        && signature[3] == 0x04);
            }
        }

        return false;
    }

    public static void zipDirectory(String sourceDirPath, String output) throws
            IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        try (fileOutputStream; zipOutputStream) {
            File sourceDir = new File(sourceDirPath);

            if (!sourceDir.isDirectory()) {
                throw new IllegalArgumentException("sourceDirPath must be a directory");
            }

            zipFilesRecursively(sourceDir, sourceDir, zipOutputStream);
        }
    }

    public static void unzip(String zipPath, String destDir) throws IOException {
        File destDirectory = new File(destDir);

        if (!destDirectory.exists()) {
            Files.createDirectories(destDirectory.toPath());
        }

        FileInputStream fileInputStream = new FileInputStream(zipPath);
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);

        try (fileInputStream; zipInputStream) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File newFile = unzipFile(destDirectory, zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Cannot create directory: " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();

                    if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
                        throw new IOException("Cannot create directory: " + parent);
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;

                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, len);
                        }
                    }
                }

                zipInputStream.closeEntry();
            }
        }
    }

    private static void zipFilesRecursively(
            File rootDir,
            File currentDir,
            ZipOutputStream zipOutputStream)
            throws IOException {
        File[] files = currentDir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();

            if (file.isDirectory()) {
                zipFilesRecursively(rootDir, file, zipOutputStream);
            } else {
                zipFile(file, relativePath, zipOutputStream);
            }
        }
    }

    private static void zipFile(File file, String zipEntryName, ZipOutputStream zipOutputStream)
            throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipOutputStream.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }

            zipOutputStream.closeEntry();
        }
    }

    private static File unzipFile(File destDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destDir, zipEntry.getName());

        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Invalid record in archive: " + zipEntry.getName());
        }

        return destFile;
    }
}
