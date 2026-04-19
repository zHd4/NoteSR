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
 * It migrates the database and file blobs (fragments) to the new encryption.
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

    /**
     * Updates the crypto secrets (master key and password) and migrates all encrypted data.
     * It performs a migration of the database and file blobs to the new encryption settings.
     *
     * @param newSecrets The new crypto secrets to be applied.
     * @throws EncryptionFailedException If encryption of data with the new secrets fails.
     * @throws DecryptionFailedException If decryption of data with the current secrets fails.
     * @throws IOException               If an I/O error occurs during the migration process.
     */
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

    /**
     * Ensures that the specified directory exists.
     * If the directory does not exist, it attempts to create it along with any necessary
     * parent directories.
     *
     * @param dir The directory to ensure existence of.
     * @throws IOException If the directory does not exist and could not be created.
     */
    private void ensureDirectoryExists(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Migrates the database and file blobs from the current encryption to the new encryption.
     *
     * @param currentKey      The current database encryption key.
     * @param newKey          The new database encryption key.
     * @param tempDbFile      The temporary database file to migrate data into.
     * @param currentBlobsDir The directory containing current encrypted file blobs.
     * @param tempBlobsDir    The temporary directory to store newly encrypted file blobs.
     * @param currentCryptor  The cryptor used for decrypting current data.
     * @param newCryptor      The cryptor used for encrypting data with new secrets.
     * @throws IOException               If an I/O error occurs.
     * @throws EncryptionFailedException If encryption fails.
     * @throws DecryptionFailedException If decryption fails.
     */
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
     * @param oldDb           The source database to retrieve blob information from.
     * @param currentBlobsDir The source directory for file blobs.
     * @param tempBlobsDir    The destination directory for re-encrypted file blobs.
     * @param currentCryptor  The cryptor used to decrypt current blobs.
     * @param newCryptor      The cryptor used to encrypt blobs with new secrets.
     * @throws IOException               If an I/O error occurs.
     * @throws EncryptionFailedException If encryption fails.
     * @throws DecryptionFailedException If decryption fails.
     */
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

    /**
     * Swaps the current database and blob directory with the temporary ones.
     * This operation attempts to be as atomic as possible to maintain data integrity.
     *
     * @param currentDbFile   The current database file.
     * @param tempDbFile      The temporary (newly migrated) database file.
     * @param currentBlobsDir The current blobs directory.
     * @param tempBlobsDir    The temporary (newly migrated) blobs directory.
     * @param oldBlobsDir     The directory where the old blobs will be moved to.
     * @throws IOException If an I/O error occurs during the swap.
     */
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

    /**
     * Replaces the current database files with the new ones.
     * It creates a temporary backup of the old database files before replacing them.
     *
     * @param currentDbFile The current main database file.
     * @param newDbFile     The new main database file.
     * @throws IOException If an I/O error occurs during file replacement.
     */
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

    /**
     * Cleans up temporary data created during the migration process.
     *
     * @param tempDbFile   The temporary database file.
     * @param tempBlobsDir The temporary blobs directory.
     * @throws IOException If an I/O error occurs during cleanup.
     */
    private void cleanupTempData(File tempDbFile, File tempBlobsDir) throws IOException {
        eraseFiles(getAllDbFiles(tempDbFile));

        File[] tempBlobs = tempBlobsDir.listFiles();
        if (tempBlobs != null) {
            eraseFiles(tempBlobs);
        }
    }

    /**
     * Returns an array of all files associated with a SQLite database.
     * This includes the main database file, the SHM file, and the WAL file.
     *
     * @param dbFile The main database file.
     * @return An array containing the main database file and its associated auxiliary files.
     */
    private File[] getAllDbFiles(File dbFile) {
        return new File[] {
            dbFile,
            new File(dbFile.getAbsolutePath() + "-shm"),
            new File(dbFile.getAbsolutePath() + "-wal")
        };
    }

    /**
     * Erases the specified files or directories.
     * If a file is a directory, it is wiped recursively.
     *
     * @param files An array of files or directories to be erased.
     * @throws IOException If an I/O error occurs during erasure.
     */
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
