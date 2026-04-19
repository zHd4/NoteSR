/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.WiperAdapter;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.service.file.FileService;
import lombok.RequiredArgsConstructor;

/**
 * Service for updating crypto secrets (master key and password).
 * It migrates the database and file blobs to the new encryption.
 */
@RequiredArgsConstructor
public final class SecretsUpdateService {

    private static final String TEMP_DB_NAME = "tmp_notesr.db";
    private static final String TEMP_BLOBS_DIR_NAME = "fblobs_tmp";
    private static final String OLD_BLOBS_DIR_NAME = "fblobs_old";

    private final Context context;
    private final String dbName;
    private final CryptoManager cryptoManager;
    private final FilesUtilsAdapter filesUtils;
    private final WiperAdapter wiper;
    private final DatabaseManager databaseManager;

    public void updateSecrets(CryptoSecrets newSecrets)
            throws EncryptionFailedException, DecryptionFailedException, IOException {

        CryptoSecrets currentSecrets = cryptoManager.getSecrets();

        try {
            AesCryptor currentCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(currentSecrets));
            AesCryptor newCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(newSecrets));

            File currentDbFile = filesUtils.getDatabaseFile(context, dbName);
            File tempDbFile = filesUtils.getDatabaseFile(context, TEMP_DB_NAME);

            File currentBlobsDir = filesUtils.getInternalFile(context, FileService.BLOBS_DIR_NAME);
            File tempBlobsDir = filesUtils.getInternalFile(context, TEMP_BLOBS_DIR_NAME);
            File oldBlobsDir = filesUtils.getInternalFile(context, OLD_BLOBS_DIR_NAME);

            ensureDirectoryExists(tempBlobsDir);

            databaseManager.closeProvider();

            migrateData(
                    currentSecrets.getKey(),
                    newSecrets.getKey(),
                    tempDbFile,
                    currentBlobsDir,
                    tempBlobsDir,
                    currentCryptor,
                    newCryptor
            );

            performAtomicSwap(currentDbFile, tempDbFile, currentBlobsDir, tempBlobsDir, oldBlobsDir);

            cryptoManager.setSecrets(context, newSecrets);
            databaseManager.reinitProvider(newSecrets.getKey());
            wiper.wipeDir(oldBlobsDir);
        } finally {
            currentSecrets.destroy();
            newSecrets.destroy();
        }
    }

    private void ensureDirectoryExists(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    private void migrateData(
            byte[] currentKey,
            byte[] newKey,
            File tempDbFile,
            File currentBlobsDir,
            File tempBlobsDir,
            AesCryptor currentCryptor,
            AesCryptor newCryptor
    ) throws IOException, EncryptionFailedException, DecryptionFailedException {

        AppDatabase currentDb = databaseManager.getDatabase(dbName, currentKey);
        AppDatabase tempDb = databaseManager.getDatabase(tempDbFile.getName(), newKey);

        if (!databaseManager.isDbAvailable(tempDb)) {
            tempDb.close();
            cleanupTempData(tempDbFile, tempBlobsDir);
            tempDb = databaseManager.getDatabase(tempDbFile.getName(), newKey);
        }

        try {
            copyDbData(currentDb, tempDb);
            updateBlobsData(currentDb, currentBlobsDir, tempBlobsDir, currentCryptor, newCryptor);
        } finally {
            currentDb.close();
            tempDb.close();
        }
    }

    private void copyDbData(AppDatabase currentDb, AppDatabase newDb) {
        newDb.runInTransaction(() -> {
            newDb.getNoteDao().insertAll(currentDb.getNoteDao().getAll());
            newDb.getFileInfoDao().insertAll(currentDb.getFileInfoDao().getAll());
            newDb.getFileBlobInfoDao().insertAll(currentDb.getFileBlobInfoDao().getAll());
            return null;
        });
    }

    private void updateBlobsData(
            AppDatabase oldDb,
            File currentBlobsDir,
            File tempBlobsDir,
            AesCryptor currentCryptor,
            AesCryptor newCryptor
    ) throws IOException, EncryptionFailedException, DecryptionFailedException {

        List<FileBlobInfo> blobsInfo = oldDb.getFileBlobInfoDao().getAll();

        for (FileBlobInfo blobInfo : blobsInfo) {
            File sourceFile = new File(currentBlobsDir, blobInfo.getId());
            File targetFile = new File(tempBlobsDir, blobInfo.getId());

            if (targetFile.exists() && targetFile.length() > 0) {
                continue;
            }

            byte[] data = filesUtils.readFileBytes(sourceFile);

            try {
                data = currentCryptor.decrypt(data);
            } catch (GeneralSecurityException e) {
                throw new DecryptionFailedException(e);
            }

            try {
                data = newCryptor.encrypt(data);
            } catch (GeneralSecurityException e) {
                throw new EncryptionFailedException(e);
            }

            filesUtils.writeFileBytes(targetFile, data);
        }
    }

    private void performAtomicSwap(
            File currentDbFile,
            File tempDbFile,
            File currentBlobsDir,
            File tempBlobsDir,
            File oldBlobsDir
    ) throws IOException {

        if (oldBlobsDir.exists()) {
            wiper.wipeDir(oldBlobsDir);
        }

        Files.move(currentBlobsDir.toPath(), oldBlobsDir.toPath());
        Files.move(tempBlobsDir.toPath(), currentBlobsDir.toPath());

        replaceDatabase(currentDbFile, tempDbFile);
    }

    private void replaceDatabase(File currentDbFile, File newDbFile) throws IOException {
        File[] currentDbFiles = getAllDbFiles(currentDbFile);
        File[] oldDbFiles = Arrays.stream(currentDbFiles)
                .map(file -> new File(file.getAbsolutePath() + ".old"))
                .toArray(File[]::new);

        for (int i = 0; i < currentDbFiles.length; i++) {
            if (currentDbFiles[i].exists()) {
                Files.move(currentDbFiles[i].toPath(), oldDbFiles[i].toPath());
            }
        }

        Files.move(newDbFile.toPath(), currentDbFile.toPath());

        eraseFiles(oldDbFiles);
    }

    private void cleanupTempData(File tempDbFile, File tempBlobsDir) throws IOException {
        eraseFiles(getAllDbFiles(tempDbFile));

        File[] tempBlobs = tempBlobsDir.listFiles();
        if (tempBlobs != null) {
            eraseFiles(tempBlobs);
        }
    }

    private File[] getAllDbFiles(File dbFile) {
        return new File[] {
            dbFile,
            new File(dbFile.getAbsolutePath() + "-shm"),
            new File(dbFile.getAbsolutePath() + "-wal")
        };
    }

    private void eraseFiles(File[] files) throws IOException {
        for (File file : files) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    wiper.wipeDir(file);
                } else {
                    wiper.wipeFile(file);
                }
            }
        }
    }
}
