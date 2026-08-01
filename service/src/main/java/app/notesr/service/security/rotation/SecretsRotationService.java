/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.rotation;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.CryptoSecretsValidator;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.service.file.FileService;
import app.notesr.service.security.AppSecurityService;
import lombok.RequiredArgsConstructor;

/**
 * Service for updating crypto secrets, such as the master key and password.
 * <p>
 * It handles the migration of the database and encrypted file blobs (fragments)
 * to the new encryption settings in a transactional manner.
 */
@RequiredArgsConstructor
public final class SecretsRotationService {

    private final Context context;
    private final AppSecurityService appSecurityService;

    /**
     * Updates the password in the crypto secrets.
     * After the update, the new password are securely cleared to minimize sensitive data
     * exposure in memory.
     *
     * @param newPassword The new password to set.
     * @throws IllegalArgumentException If the new password is invalid.
     * @throws SecretsRotationFailedException If the password update fails.
     */
    public void updatePassword(char[] newPassword) {
        try {
            CryptoSecretsValidator.validatePassword(newPassword);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid password", e);
        }

        CryptoSecrets currentSecrets = null;

        try {
            currentSecrets = appSecurityService.getActualSecrets();
            currentSecrets.setPassword(newPassword);
            appSecurityService.setSecrets(CryptoSecrets.from(currentSecrets));
        } catch (Exception e) {
            throw new SecretsRotationFailedException("Failed to update password", e);
        } finally {
            if (currentSecrets != null) {
                // Also fills the new password with \0
                currentSecrets.destroy();
            }
        }
    }

    /**
     * Updates the crypto secrets (master key and password) and migrates all encrypted data.
     * This could be heavy and long-term operation, so it should be executed
     * using {@link SecretsUpdateAndroidServiceStarter}.
     * <p>
     * It performs a migration of the database and file blobs to the new encryption settings.
     * After the migration, the newSecrets are destroyed to minimize sensitive data
     * exposure in memory.
     *
     * @param txFiles                         The transactional files utility.
     * @param databaseManager                 The database manager for handling database operations.
     * @param dbName                          The name of the database file.
     * @param stateHolder                     The state holder for tracking rotation progress.
     * @param newSecrets                      The new crypto secrets to be applied.
     *
     * @throws IllegalArgumentException If the new secrets are invalid.
     * @throws SecretsRotationFailedException If the secrets rotation fails.
     * @see SecretsUpdateAndroidService
     * @see SecretsUpdateAndroidServiceStarter
     */
    public void updateSecrets(
            TransactionalFilesUtil txFiles,
            DatabaseManager databaseManager,
            String dbName,
            SecretsUpdateStateHolder stateHolder,
            CryptoSecrets newSecrets) {

        try {
            CryptoSecretsValidator.validate(newSecrets);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid new secrets", e);
        }

        CryptoSecrets currentSecrets = null;

        try (txFiles) {
            currentSecrets = appSecurityService.getActualSecrets();

            if (getStatus(stateHolder) == null) {
                setStatus(stateHolder, SecretsRotationStatus.INITIALIZING);
            }

            if (getStatus(stateHolder) == SecretsRotationStatus.DONE) {
                return;
            }

            if (getStatus(stateHolder) == SecretsRotationStatus.FAILED) {
                throw new SecretsRotationFailedException("Secrets rotation is already failed");
            }

            databaseManager.closeProvider();

            if (!txFiles.isCommitted()) {
                AesCryptor currentCryptor = AesCryptorFactory.createAesGcmCryptor(currentSecrets);
                AesCryptor newCryptor = AesCryptorFactory.createAesGcmCryptor(newSecrets);
                File currentBlobsDir = txFiles.getInternalFile(context, FileService.BLOBS_DIR_NAME);

                migrateData(
                        txFiles,
                        databaseManager,
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
                if (getStatus(stateHolder).isBefore(SecretsRotationStatus.DONE)) {
                    setStatus(stateHolder, SecretsRotationStatus.DONE);
                }
            }

            appSecurityService.setSecrets(CryptoSecrets.from(newSecrets));
            setStatus(stateHolder, SecretsRotationStatus.DONE);

            databaseManager.reinitProvider(newSecrets.getKey());
        } catch (Exception e) {
            txFiles.rollback();
            setStatus(stateHolder, SecretsRotationStatus.FAILED);
            throw new SecretsRotationFailedException("Secrets rotation failed", e);
        } finally {
            if (currentSecrets != null) {
                currentSecrets.destroy();
            }

            newSecrets.destroy();
        }
    }

    /**
     * Migrates the database and file blobs from the current encryption settings to the new ones.
     *
     * @param txFiles         The transactional files utility.
     * @param databaseManager The database manager for handling database operations.
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
    void migrateData(
            TransactionalFilesUtil txFiles,
            DatabaseManager databaseManager,
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
            if (getStatus(stateHolder).isBeforeOrEqual(SecretsRotationStatus.MOVING_BLOBS_DATA)) {
                setStatus(stateHolder, SecretsRotationStatus.MOVING_BLOBS_DATA);
                updateBlobsData(txFiles, currentDb, currentBlobsDir, currentCryptor, newCryptor);
            }

            if (getStatus(stateHolder).isBeforeOrEqual(SecretsRotationStatus.MOVING_DB_DATA)) {
                // Staging files for new database
                File stagedDbFile = txFiles.stageFile(currentDbFile);

                // Creating empty database
                if (!stagedDbFile.delete()) {
                    throw new IOException("Failed to delete staged database file: "
                            + stagedDbFile.getAbsolutePath());
                }

                var tempDb = databaseManager.getDatabase(stagedDbFile.getAbsolutePath(), newKey);

                try {
                    setStatus(stateHolder, SecretsRotationStatus.MOVING_DB_DATA);
                    copyDbData(currentDb, tempDb);
                } finally {
                    tempDb.close();
                }

                // Deleting WAL and SHM files of the original database
                // We must use the original file path because they might not be staged yet
                File originalDbFile = context.getDatabasePath(dbName);
                txFiles.deleteFile(new File(originalDbFile.getPath() + "-shm"));
                txFiles.deleteFile(new File(originalDbFile.getPath() + "-wal"));
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
    void copyDbData(AppDatabase currentDb, AppDatabase newDb) {
        newDb.runInTransaction(() -> {
            newDb.getNoteDao().insertAll(currentDb.getNoteDao().getAll());
            newDb.getFileInfoDao().insertAll(currentDb.getFileInfoDao().getAll());
            newDb.getFileBlobInfoDao().insertAll(currentDb.getFileBlobInfoDao().getAll());
            return null;
        });
    }

    /**
     * Re-encrypts all file blobs within the blobs directory using the new cryptor.
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
    void updateBlobsData(
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
                try {
                    // If it can be decrypted with new cryptor, it's already migrated
                    decryptBlobData(newCryptor, data);
                    continue;
                } catch (DecryptionFailedException ignored) {
                    // Not migrated yet
                }
            }

            data = decryptBlobData(currentCryptor, data);
            data = encryptBlobData(newCryptor, data);

            txFiles.writeFileBytes(sourceFile, data);
        }
    }

    /**
     * Encrypts blob data using the provided cryptor.
     *
     * @param cryptor The cryptor to use for encryption.
     * @param data    The raw data to encrypt.
     * @return The encrypted data.
     * @throws EncryptionFailedException If the encryption process fails.
     */
    byte[] encryptBlobData(AesCryptor cryptor, byte[] data)
            throws EncryptionFailedException {

        try {
            return cryptor.encrypt(data);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Decrypts blob data using the provided cryptor.
     *
     * @param cryptor The cryptor to use for decryption.
     * @param data    The encrypted data.
     * @return The decrypted raw data.
     * @throws DecryptionFailedException If the decryption process fails.
     */
    byte[] decryptBlobData(AesCryptor cryptor, byte[] data)
            throws DecryptionFailedException {

        try {
            return cryptor.decrypt(data);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    /**
     * Retrieves the current status from the state holder.
     *
     * @param stateHolder The state holder.
     * @return The current {@link SecretsRotationStatus}.
     */
    SecretsRotationStatus getStatus(SecretsUpdateStateHolder stateHolder) {
        return stateHolder.getState().getStatus();
    }

    /**
     * Updates the status in the state holder.
     *
     * @param stateHolder The state holder.
     * @param status      The new status to set.
     */
    void setStatus(SecretsUpdateStateHolder stateHolder, SecretsRotationStatus status) {
        stateHolder.setState(stateHolder.getState().setStatus(status));
    }
}
