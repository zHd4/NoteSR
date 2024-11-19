package app.notesr.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static boolean isZipArchive(String path) throws IOException {
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            return false;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] signature = new byte[4];

            if (fileInputStream.read(signature) == 4) {
                return (signature[0] == 0x50 && signature[1] == 0x4B &&
                        signature[2] == 0x03 && signature[3] == 0x04);
            }
        }

        return false;
    }

    public static void zipDirectory(String sourceDirPath, String output, Thread thread) throws
            IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            File sourceDir = new File(sourceDirPath);
            zipFilesRecursively(sourceDir, "", zipOutputStream, thread);
        }
    }

    public static void unzip(String sourcePath, String destDir, Thread thread) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        try (FileInputStream fileInputStream = new FileInputStream(sourcePath);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {

            ZipEntry entry = zipInputStream.getNextEntry();

            while (entry != null) {
                if (thread != null && thread.isInterrupted()) {
                    return;
                }

                String filePath = destDir + File.separator + entry.getName();

                if (entry.isDirectory()) {
                    new File(filePath).mkdirs();
                } else {
                    File file = new File(filePath);
                    file.getParentFile().mkdirs();

                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;

                        while ((length = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private static void zipFilesRecursively(
            File file,
            String fileName,
            ZipOutputStream zipOutputStream,
            Thread thread)
            throws IOException {
        if (thread != null && thread.isInterrupted()) {
            return;
        }

        if (file.isDirectory()) {
            if (!fileName.endsWith("/")) {
                fileName += "/";
            }

            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            zipOutputStream.closeEntry();

            File[] children = file.listFiles();

            if (children != null) {
                for (File childFile : children) {
                    zipFilesRecursively(
                            childFile,
                            fileName + childFile.getName(),
                            zipOutputStream,
                            thread
                    );
                }
            }

            return;
        }

        zipFile(file, zipOutputStream);
    }

    private static void zipFile(File file, ZipOutputStream zipOutputStream) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(entry);

            byte[] bytes = new byte[1024];
            int length;

            while ((length = fileInputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
