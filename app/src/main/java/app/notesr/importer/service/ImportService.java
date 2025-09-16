package app.notesr.importer.service;

import static java.util.UUID.randomUUID;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import app.notesr.file.service.FileService;
import app.notesr.importer.service.v1.ImportV1Strategy;
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

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final ContentResolver contentResolver;
    private final Uri backupUri;
    private final ImportStatusCallback statusCallback;
    private final WiperAdapter wiper;

    public void doImport() {
        statusCallback.updateStatus(ImportStatus.DECRYPTING);
        File tempDecryptedBackupFile = new File(context.getCacheDir(), randomUUID().toString());

        try {
            decrypt(tempDecryptedBackupFile);
            ImportStrategy importStrategy;

            if (ZipUtils.isZipArchive(tempDecryptedBackupFile.getAbsolutePath())) {
                importStrategy = getV2Strategy(tempDecryptedBackupFile);
            } else {
                importStrategy = getV1Strategy(tempDecryptedBackupFile);
            }

            importStrategy.execute();
            statusCallback.updateStatus(ImportStatus.DONE);
        } catch (Throwable e) {
            if (tempDecryptedBackupFile.exists()) {
                wipeFile(tempDecryptedBackupFile);
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
                statusCallback, TIMESTAMP_FORMATTER);
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
}
