package app.notesr.importer.service;

import static java.util.UUID.randomUUID;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import app.notesr.file.service.FileService;
import app.notesr.importer.service.v1.ImportV1Strategy;
import app.notesr.importer.service.v3.ImportV3Strategy;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.BackupDecryptor;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.db.AppDatabase;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.importer.service.v2.ImportV2Strategy;
import app.notesr.util.WiperAdapter;
import app.notesr.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TAG = ImportService.class.getName();
    private static final String VERSION_FILENAME = "version";
    private static final String MIN_APP_VERSION_FOR_V3_STRATEGY = "5.1.1";

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final CryptoSecrets cryptoSecrets;
    private final ContentResolver contentResolver;
    private final Uri backupUri;
    private final ImportStatusCallback statusCallback;
    private final WiperAdapter wiper;

    private File tempDir;

    public void doImport() {
        statusCallback.updateStatus(ImportStatus.DECRYPTING);
        File tempDecryptedBackupFile = new File(context.getCacheDir(), randomUUID().toString());

        try {
            decrypt(tempDecryptedBackupFile);
            ImportStrategy importStrategy;

            if (ZipUtils.isZipArchive(tempDecryptedBackupFile.getAbsolutePath())) {
                String appVersionFromBackup = readFileLineFromZip(tempDecryptedBackupFile,
                        VERSION_FILENAME);

                if (compareVersions(appVersionFromBackup, MIN_APP_VERSION_FOR_V3_STRATEGY) >= 0) {
                    importStrategy = getV3Strategy(tempDecryptedBackupFile);
                } else {
                    tempDir = new File(context.getCacheDir(), randomUUID().toString());
                    importStrategy = getV2Strategy(tempDecryptedBackupFile);
                }
            } else {
                importStrategy = getV1Strategy(tempDecryptedBackupFile);
            }

            importStrategy.execute();
            statusCallback.updateStatus(ImportStatus.DONE);
        } catch (Throwable e) {
            if (tempDecryptedBackupFile.exists()) {
                wipeFile(tempDecryptedBackupFile);
            }

            if (tempDir != null && tempDir.exists()) {
                wipeDir(tempDir);
            }

            ImportStatus fail = e instanceof DecryptionFailedException
                    ? ImportStatus.DECRYPTION_FAILED
                    : ImportStatus.IMPORT_FAILED;

            statusCallback.updateStatus(fail);
        }
    }

    private ImportV1Strategy getV1Strategy(File tempDecryptedFile) {
        return new ImportV1Strategy(db, noteService, fileService, tempDecryptedFile,
                statusCallback, TIMESTAMP_FORMATTER);
    }

    private ImportV2Strategy getV2Strategy(File tempDecryptedFile) {
        return new ImportV2Strategy(context, db, noteService, fileService, tempDecryptedFile,
                tempDir, statusCallback, TIMESTAMP_FORMATTER);
    }

    private ImportV3Strategy getV3Strategy(File tempDecryptedFile) {
        return new ImportV3Strategy(cryptoSecrets, db, noteService, fileService, tempDecryptedFile,
                statusCallback);
    }

    private void decrypt(File outputFile)
            throws DecryptionFailedException {
        try {
            CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
            CryptoSecrets cryptoSecrets = cryptoManager.getSecrets();

            BackupDecryptor decryptor = new BackupDecryptor(
                    contentResolver,
                    cryptoSecrets,
                    backupUri,
                    outputFile
            );

            decryptor.decrypt();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new DecryptionFailedException();
        }
    }

    private void wipeFile(File file) {
        try {
            wiper.wipeFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void wipeDir(File dir) {
        try {
            wiper.wipeDir(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFileLineFromZip(File zipArchive, String fileName) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipArchive)) {
            ZipEntry entry = zipFile.getEntry(fileName);

            if (entry == null) {
                throw new IOException(fileName + " not found in" + zipArchive.getAbsolutePath());
            }

            try (InputStream is = zipFile.getInputStream(entry);
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.readLine();
            }
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }
}
