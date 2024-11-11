package app.notesr.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.util.UUID.randomUUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileInputStream;

class ZipUtilsTest {
    private static final String DIR_PATH = generateFixturePath("exported");
    private static final String ZIP_PATH = generateFixturePath("exported.zip");

    private static final String TEMP_EXTRACTED_DIR_PATH = generateTempPath(randomUUID().toString());
    private static final String TEMP_ZIP_PATH = generateTempPath(randomUUID().toString() + ".zip");

    @Test
    public void testZipDirectory() throws IOException, NoSuchAlgorithmException {
        ZipUtils.zipDirectory(DIR_PATH, TEMP_ZIP_PATH);
        File zipFile = new File(TEMP_ZIP_PATH);

        assertTrue(zipFile.exists(), "Zip file not found");
        assertTrue(isFilesIdentical(ZIP_PATH, TEMP_ZIP_PATH), "Hash mismatch");
    }

    @Test
    public void testUnzip() throws IOException, NoSuchAlgorithmException {
        ZipUtils.unzip(ZIP_PATH, TEMP_EXTRACTED_DIR_PATH);
        File dir = new File(TEMP_EXTRACTED_DIR_PATH);

        assertTrue(dir.exists(), "Extract directory not found");
        assertTrue(isDirsIdentical(DIR_PATH, TEMP_EXTRACTED_DIR_PATH));
    }

    @AfterAll
    public static void afterAll() {
        File tempZipFile = new File(TEMP_ZIP_PATH);

        tempZipFile.delete();
        removeDir(new File(TEMP_EXTRACTED_DIR_PATH));
    }

    private static String generateFixturePath(String pathPart) {
        return Path.of("src/test/resources/fixtures", pathPart).toString();
    }

    private static String generateTempPath(String pathPart) {
        return Path.of(System.getProperty("java.io.tmpdir"), pathPart).toString();
    }

    private static boolean isDirsIdentical(String path1, String path2) throws IOException,
            NoSuchAlgorithmException {
        File dir1 = new File(path1);
        File dir2 = new File(path2);

        if (!dir1.isDirectory() || !dir2.isDirectory()) {
            throw new IllegalArgumentException("Both inputs must be directories");
        }

        File[] dir1Files = dir1.listFiles();
        File[] dir2Files = dir2.listFiles();

        if (dir1Files == null || dir2Files == null || dir1Files.length != dir2Files.length) {
            return false;
        }

        for (File file1 : dir1Files) {
            File file2 = new File(dir2, file1.getName());

            if (file1.isDirectory()) {
                if (!file2.exists() || !file2.isDirectory()
                        || !isDirsIdentical(file1.getPath(), file2.getPath())) {
                    return false;
                }
            } else {
                if (!file2.exists() || !file2.isFile()
                        || !isFilesIdentical(file1.getPath(), file2.getPath())) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isFilesIdentical(String path1, String path2) throws IOException,
            NoSuchAlgorithmException {
        return sha256OfFile(path1).equals(sha256OfFile(path2));
    }

    private static String sha256OfFile(String path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream inputStream = new FileInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hex = new StringBuilder(hashBytes.length * 2);

        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }

        return hex.toString();
    }

    private static void removeDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();

            if (children != null) {
                for (File child : children) {
                    removeDir(child);
                }
            }
        }

        dir.delete();
    }
}