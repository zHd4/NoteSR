/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.service.file.FileService;
import lombok.RequiredArgsConstructor;

/**
 * Service for updating crypto secrets (master key and password).
 * It migrates the database and file blobs (fragments) to the new encryption.
 */
@RequiredArgsConstructor
public final class SecretsUpdateService {

    private final Context context;
    private final DatabaseManager databaseManager;

    /**
     * Updates the crypto secrets (master key and password) and migrates all encrypted data.
     * It performs a migration of the database and file blobs to the new encryption settings.
     *
     * @param newSecrets The new crypto secrets to be applied.
     * @throws SecretsUpdateFailedException If the secrets update fails.
     */
    public void updateSecrets(
            TransactionalFilesUtil txFiles,
            CryptoManager cryptoManager,
            String dbName,
            SecretsUpdateStateHolder stateHolder,
            CryptoSecrets newSecrets) {

        var currentSecrets = cryptoManager.getSecrets();

        try (txFiles) {
            if (getStatus(stateHolder) == null) {
                setStatus(stateHolder, SecretsUpdateStatus.INITIALIZING);
            }

            if (getStatus(stateHolder) == SecretsUpdateStatus.DONE) {
                return;
            }

            if (getStatus(stateHolder) == SecretsUpdateStatus.FAILED) {
                throw new SecretsUpdateFailedException("Secrets update is already failed");
            }

            databaseManager.closeProvider();

            if (!txFiles.isCommitted()) {
                var currentCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(currentSecrets));
                var newCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(newSecrets));
                var currentBlobsDir = txFiles.getInternalFile(context, FileService.BLOBS_DIR_NAME);

                migrateData(
                        txFiles,
                        stateHolder,
                        dbName,
                        currentSecrets.getKey(),
                        newSecrets.getKey(),
                        currentBlobsDir,
                        currentCryptor,
                        newCryptor
                );

                txFiles.commit();
            } else {
                if (getStatus(stateHolder).isBefore(SecretsUpdateStatus.DONE)) {
                    setStatus(stateHolder, SecretsUpdateStatus.DONE);
                }
            }

            cryptoManager.setSecrets(context, newSecrets);
            setStatus(stateHolder, SecretsUpdateStatus.DONE);

            databaseManager.reinitProvider(newSecrets.getKey());
        } catch (Exception e) {
            txFiles.rollback();
            setStatus(stateHolder, SecretsUpdateStatus.FAILED);
            throw new SecretsUpdateFailedException("Secrets update failed", e);
        } finally {
            currentSecrets.destroy();
            newSecrets.destroy();
        }
    }

    /**
     * Migrates the database and file blobs from the current encryption to the new encryption.
     *
     * @param txFiles         The transactional files utility.
     * @param stateHolder     The state holder for the update process.
     * @param dbName          The name of the database to migrate.
     * @param currentKey      The current database encryption key.
     * @param newKey          The new database encryption key.
     * @param currentBlobsDir The directory containing current encrypted file blobs.
     * @param currentCryptor  The cryptor used for decrypting current data.
     * @param newCryptor      The cryptor used for encrypting data with new secrets.
     * @throws IOException               If an I/O error occurs.
     * @throws EncryptionFailedException If encryption fails.
     * @throws DecryptionFailedException If decryption fails.
     */
    private void migrateData(
            TransactionalFilesUtil txFiles,
            SecretsUpdateStateHolder stateHolder,
            String dbName,
            byte[] currentKey,
            byte[] newKey,
            File currentBlobsDir,
            AesCryptor currentCryptor,
            AesCryptor newCryptor)
            throws IOException, EncryptionFailedException, DecryptionFailedException {

        // Check if database is already migrated
        if (databaseManager.isDbAvailable(databaseManager.getDatabase(dbName, newKey))) {
            return;
        }

        var currentDb = databaseManager.getDatabase(dbName, currentKey);
        var currentDbFile = txFiles.getDatabaseFile(context, dbName);

        try {
            if (getStatus(stateHolder).isBeforeOrEqual(SecretsUpdateStatus.MOVING_BLOBS_DATA)) {
                setStatus(stateHolder, SecretsUpdateStatus.MOVING_BLOBS_DATA);
                updateBlobsData(txFiles, currentDb, currentBlobsDir, currentCryptor, newCryptor);
            }

            if (getStatus(stateHolder).isBeforeOrEqual(SecretsUpdateStatus.MOVING_DB_DATA)) {
                // Staging files for new database
                File stagedDbFile = txFiles.stageFile(currentDbFile);

                // Creating empty database
                if (!stagedDbFile.delete()) {
                    throw new IOException("Failed to delete staged database file: "
                            + stagedDbFile.getAbsolutePath());
                }

                var tempDb = databaseManager.getDatabase(stagedDbFile.getAbsolutePath(), newKey);

                try {
                    setStatus(stateHolder, SecretsUpdateStatus.MOVING_DB_DATA);
                    copyDbData(currentDb, tempDb);
                } finally {
                    tempDb.close();
                }

                // TODO: Check if we need to delete the files
                txFiles.deleteFile(new File(currentDbFile.getPath() + "-shm"));
                txFiles.deleteFile(new File(currentDbFile.getPath() + "-wal"));
            }
        } finally {
            currentDb.close();
        }
    }

    /**
     * Copies all data from the current database to the new database.
     *
     * @param currentDb The source database.
     * @param newDb     The destination database.
     */
    private void copyDbData(AppDatabase currentDb, AppDatabase newDb) {
        newDb.runInTransaction(() -> {
            newDb.getNoteDao().insertAll(currentDb.getNoteDao().getAll());
            newDb.getFileInfoDao().insertAll(currentDb.getFileInfoDao().getAll());
            newDb.getFileBlobInfoDao().insertAll(currentDb.getFileBlobInfoDao().getAll());
            return null;
        });
    }

    /**
     * Re-encrypts all file blobs from the current directory to the temporary directory.
     *
     * @param txFiles         The transactional files utility.
     * @param oldDb           The source database to retrieve blob information from.
     * @param currentBlobsDir The source directory for file blobs.
     * @param currentCryptor  The cryptor used to decrypt current blobs.
     * @param newCryptor      The cryptor used to encrypt blobs with new secrets.
     * @throws IOException               If an I/O error occurs.
     * @throws EncryptionFailedException If encryption fails.
     * @throws DecryptionFailedException If decryption fails.
     */
    private void updateBlobsData(
            TransactionalFilesUtil txFiles,
            AppDatabase oldDb,
            File currentBlobsDir,
            AesCryptor currentCryptor,
            AesCryptor newCryptor
    ) throws IOException, EncryptionFailedException, DecryptionFailedException {

        List<FileBlobInfo> blobsInfo = oldDb.getFileBlobInfoDao().getAll();

        for (FileBlobInfo blobInfo : blobsInfo) {
            var sourceFile = new File(currentBlobsDir, blobInfo.getId());

            // If it already staged, it has already been processed
            boolean isStaged = txFiles.isStaged(sourceFile);

            // In transaction, we overwrite the same path, but it's staged
            byte[] data = txFiles.readFileBytes(sourceFile);

            if (isStaged) {
                // Trying to check if already re-encrypted blob can be successfully
                // decrypted and skip if it can, otherwise it will be failed
                decryptBlobData(newCryptor, data);
                continue;
            }

            data = decryptBlobData(currentCryptor, data);
            data = encryptBlobData(newCryptor, data);

            txFiles.writeFileBytes(sourceFile, data);
        }
    }

    private byte[] encryptBlobData(AesCryptor cryptor, byte[] data)
            throws EncryptionFailedException {

        try {
            return cryptor.encrypt(data);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    private byte[] decryptBlobData(AesCryptor cryptor, byte[] data)
            throws DecryptionFailedException {

        try {
            return cryptor.decrypt(data);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    private SecretsUpdateStatus getStatus(SecretsUpdateStateHolder stateHolder) {
        return stateHolder.getState().getStatus();
    }

    private void setStatus(SecretsUpdateStateHolder stateHolder, SecretsUpdateStatus status) {
        stateHolder.setState(stateHolder.getState().setStatus(status));
    }
}
