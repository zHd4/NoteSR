package app.notesr.importer.service;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import app.notesr.importer.service.v1.ImportV1Strategy;
import app.notesr.security.crypto.BackupCryptor;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.db.AppDatabase;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.importer.service.v2.ImportV2Strategy;
import app.notesr.util.Wiper;
import app.notesr.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TAG = ImportService.class.getName();

    private final Context context;
    private final AppDatabase db;
    private final FileInputStream sourceStream;
    private final ImportStatusCallback statusCallback;

    public void doImport() {
        statusCallback.updateStatus(ImportStatus.DECRYPTING);
        File tempDecryptedFile = new File(context.getCacheDir(), randomUUID().toString());

        try {
            FileOutputStream outputStream = new FileOutputStream(tempDecryptedFile);
            decrypt(sourceStream, outputStream);

            ImportStrategy importStrategy;

            if (ZipUtils.isZipArchive(tempDecryptedFile.getAbsolutePath())) {
                importStrategy = new ImportV2Strategy(context, db, tempDecryptedFile,
                        statusCallback, TIMESTAMP_FORMATTER);
            } else {
                importStrategy = new ImportV1Strategy(db, tempDecryptedFile, statusCallback,
                        TIMESTAMP_FORMATTER);
            }

            importStrategy.execute();
        } catch (DecryptionFailedException e) {
            if (tempDecryptedFile.exists()) {
                wipeFile(tempDecryptedFile);
            }

            statusCallback.updateStatus(ImportStatus.DECRYPTION_FAILED);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void decrypt(FileInputStream inputStream, FileOutputStream outputStream) throws
            DecryptionFailedException {
        try {
            CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
            CryptoSecrets cryptoSecrets = cryptoManager.getSecrets();

            BackupCryptor backupCryptor = new BackupCryptor(inputStream, outputStream,
                    cryptoSecrets);
            backupCryptor.decrypt();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new DecryptionFailedException();
        }
    }

    private static void wipeFile(File file) {
        try {
            new Wiper().wipeFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
