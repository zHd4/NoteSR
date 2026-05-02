/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

/**
 * A transactional version of {@link FilesUtilsAdapter} that stages file modifications
 * in the application's internal cache directory.
 * Changes are only applied to the original files when {@link #commit()} is called.
 * Supports persistence via a journal file for crash recovery.
 */
public final class TransactionalFilesUtil implements FilesUtilsAdapter, AutoCloseable {

    private static final String TAG = TransactionalFilesUtil.class.getCanonicalName();
    private static final String JOURNAL_FILE_NAME = "transaction_journal.json";

    private final FilesUtilsAdapter baseUtils;
    private final File transactionDir;
    private final File journalFile;
    private final Map<File, File> stagedFiles = new HashMap<>();
    private final Set<File> deletedFiles = new HashSet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private boolean committed = false;

    /**
     * Creates a new transactional files utility.
     *
     * @param context       The application context used to access the internal cache directory.
     * @param baseUtils     The base files utility adapter for performing actual file operations.
     * @throws FilesTransactionException if transaction initialization fails.
     */
    public TransactionalFilesUtil(Context context, FilesUtilsAdapter baseUtils) {
        this(context, baseUtils, randomUUID().toString());
    }

    /**
     * Creates a new transactional files utility.
     *
     * @param context       The application context.
     * @param baseUtils     The base files utility.
     * @param transactionId A unique ID for this transaction. If a transaction with this ID
     *                      already exists on disk, it will be loaded for recovery.
     * @throws FilesTransactionException if transaction initialization fails.
     */
    public TransactionalFilesUtil(
            Context context,
            FilesUtilsAdapter baseUtils,
            String transactionId) {

        this.baseUtils = baseUtils;

        requireNonNull(context, "Context cannot be null");
        requireNonNull(baseUtils, "Base files utility cannot be null");
        requireNonNull(transactionId, "Transaction ID cannot be null");

        this.transactionDir = new File(context.getCacheDir(), "tx_" + transactionId);
        this.journalFile = new File(transactionDir, JOURNAL_FILE_NAME);

        if (!transactionDir.exists() && !transactionDir.mkdirs()) {
            throw new FilesTransactionException("Failed to create transaction directory: "
                    + transactionDir.getAbsolutePath());
        }

        loadJournal();
    }

    /**
     * Reads the content of a file. If the file has been modified within this transaction,
     * the staged version is read.
     *
     * @param file The file to read.
     * @return The byte content of the file.
     * @throws IOException If the file has been deleted in this transaction or if
     * an I/O error occurs.
     */
    @Override
    public byte[] readFileBytes(File file) throws IOException {
        if (deletedFiles.contains(file)) {
            throw new IOException("File has been deleted in this transaction: "
                    + file.getAbsolutePath());
        }

        File stagedFile = stagedFiles.get(file);
        return baseUtils.readFileBytes(stagedFile != null ? stagedFile : file);
    }

    /**
     * Writes data to a file by staging it in the transaction directory.
     *
     * @param file The target file.
     * @param data The data to write.
     * @throws IOException If an I/O error occurs during staging.
     */
    @Override
    public void writeFileBytes(File file, byte[] data) throws IOException {
        writeFileBytes(file, data, false);
    }

    /**
     * Writes data to a file by staging it, with an option to append.
     *
     * @param file   The target file.
     * @param data   The data to write.
     * @param append Whether to append to the existing staged content.
     * @throws IOException If an I/O error occurs during staging.
     */
    @Override
    public void writeFileBytes(File file, byte[] data, boolean append) throws IOException {
        File stagedFile = getOrCreateStagedFile(file);

        baseUtils.writeFileBytes(stagedFile, data, append);
        deletedFiles.remove(file);

        saveJournal();
    }

    /**
     * Stages a move or rename operation.
     *
     * @param source The source file or directory.
     * @param target The destination file or directory.
     * @throws IOException If an I/O error occurs during staging.
     */
    @Override
    public void moveFile(File source, File target) throws IOException {
        File stagedSource = stagedFiles.remove(source);

        if (stagedSource != null) {
            File stagedTarget = getStagedFileForTarget(target);

            Files.move(stagedSource.toPath(), stagedTarget.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            stagedFiles.put(target, stagedTarget);
        } else if (source.exists()) {
            File stagedTarget = getStagedFileForTarget(target);

            if (source.isDirectory()) {
                copyDirectory(source, stagedTarget);
            } else {
                Files.copy(source.toPath(), stagedTarget.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            stagedFiles.put(target, stagedTarget);
        }

        if (!source.equals(target)) {
            deletedFiles.add(source);
        }

        deletedFiles.remove(target);
        saveJournal();
    }

    /**
     * Marks a file for deletion upon commit.
     *
     * @param file The file to delete.
     */
    @Override
    public void deleteFile(File file) {
        stagedFiles.remove(file);
        deletedFiles.add(file);
        saveJournal();
    }

    /**
     * Resolves a path to a file in the internal storage, returning the staged version if present.
     *
     * @param context The application context.
     * @param path    The relative path.
     * @return The resolved (and potentially staged) file.
     */
    @Override
    public File getInternalFile(Context context, String path) {
        File originalFile = baseUtils.getInternalFile(context, path);
        return stagedFiles.getOrDefault(originalFile, originalFile);
    }

    /**
     * Resolves a database file path, returning the staged version if present.
     *
     * @param context  The application context.
     * @param filename The database filename.
     * @return The resolved (and potentially staged) file.
     */
    @Override
    public File getDatabaseFile(Context context, String filename) {
        File originalFile = baseUtils.getDatabaseFile(context, filename);
        return stagedFiles.getOrDefault(originalFile, originalFile);
    }

    /**
     * Extracts the file extension.
     */
    @Override
    public String getFileExtension(String fileName) {
        return baseUtils.getFileExtension(fileName);
    }

    /**
     * Explicitly stages a file for modification.
     *
     * @param originalFile The original file to stage.
     * @return The temporary staged file in the transaction directory.
     * @throws IOException If the file cannot be staged.
     */
    public File stageFile(File originalFile) throws IOException {
        File staged = getOrCreateStagedFile(originalFile);
        saveJournal();
        return staged;
    }

    /**
     * Checks if a file is currently part of this transaction (either as a source or a staged copy).
     */
    public boolean isStaged(File file) {
        return stagedFiles.entrySet().stream()
                .anyMatch(entry -> entry.getKey().equals(file)
                        || entry.getValue().equals(file));
    }

    /**
     * Applies all staged changes to the original files and deletes the transaction directory.
     * This operation is atomic regarding the journal state.
     *
     * @throws IOException If applying changes fails.
     */
    public void commit() throws IOException {
        if (committed) {
            return;
        }

        saveJournal(true);

        // Apply deletions
        for (File file : deletedFiles) {
            if (file.exists() && !stagedFiles.containsKey(file)) {
                deleteRecursive(file);
            }
        }

        // Apply modifications
        for (Map.Entry<File, File> entry : stagedFiles.entrySet()) {
            File originalFile = entry.getKey();
            File stagedFile = entry.getValue();

            if (stagedFile.exists()) {
                File parent = originalFile.getParentFile();

                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory: "
                            + parent.getAbsolutePath());
                }

                Files.move(stagedFile.toPath(), originalFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        cleanup();
        committed = true;
    }

    /**
     * Discards all staged changes and cleans up the transaction directory.
     */
    public void rollback() {
        if (committed) {
            return;
        }

        cleanup();
    }

    /**
     * Closes the utility. Note that this does not automatically roll back changes,
     * allowing for potential recovery of the transaction if the application was
     * interrupted.
     */
    @Override
    public void close() {
        // We don't automatically rollback in close() to allow for crash recovery.
        // If it's a non-persistent transaction (not committed/rolled back),
        // it will stay on disk until resumed or manually cleaned.
    }

    /**
     * @return The unique ID associated with this transaction.
     */
    public String getTransactionId() {
        return transactionDir.getName().substring(3);
    }

    private void cleanup() {
        try {
            if (transactionDir.exists()) {
                deleteRecursive(transactionDir);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to cleanup transaction directory", e);
        }

        stagedFiles.clear();
        deletedFiles.clear();
    }

    private void deleteRecursive(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }

        Files.deleteIfExists(file.toPath());
    }

    private File getOrCreateStagedFile(File originalFile) throws IOException {
        File stagedFile = stagedFiles.get(originalFile);

        if (stagedFile != null) {
            return stagedFile;
        }

        // If the file is already a staged file of this transaction, return it as is.
        if (originalFile.getAbsolutePath().startsWith(transactionDir.getAbsolutePath())) {
            return originalFile;
        }

        stagedFile = getStagedFileForTarget(originalFile);

        if (originalFile.exists() && !deletedFiles.contains(originalFile)) {
            if (originalFile.isDirectory()) {
                copyDirectory(originalFile, stagedFile);
            } else {
                Files.copy(originalFile.toPath(), stagedFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        stagedFiles.put(originalFile, stagedFile);
        return stagedFile;
    }

    private File getStagedFileForTarget(File targetFile) {
        String parentPath = targetFile.getParent();
        String pathHash = (parentPath != null)
                ? Integer.toHexString(parentPath.hashCode())
                : "root";

        File dir = new File(transactionDir, pathHash);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new FilesTransactionException("Failed to create staged directory: "
                    + dir.getAbsolutePath());
        }

        return new File(dir, targetFile.getName());
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Failed to create directory: " + target.getAbsolutePath());
        }

        File[] files = source.listFiles();

        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    Files.copy(file.toPath(), targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void saveJournal() {
        saveJournal(false);
    }

    private void saveJournal(boolean committing) {
        File tempJournal = new File(journalFile.getAbsolutePath() + ".tmp");

        try {
            Journal journal = new Journal();

            for (Map.Entry<File, File> entry : stagedFiles.entrySet()) {
                journal.stagedFiles.put(entry.getKey().getAbsolutePath(),
                        entry.getValue().getAbsolutePath());
            }

            for (File file : deletedFiles) {
                journal.deletedFiles.add(file.getAbsolutePath());
            }

            journal.committing = committing;

            objectMapper.writeValue(tempJournal, journal);
            Files.move(tempJournal.toPath(), journalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save transaction journal", e);
            if (tempJournal.exists()) {
                tempJournal.delete();
            }
        }
    }

    private void loadJournal() {
        if (!journalFile.exists()) {
            return;
        }

        try {
            Journal journal = objectMapper.readValue(journalFile, Journal.class);
            stagedFiles.clear();

            for (Map.Entry<String, String> entry : journal.stagedFiles.entrySet()) {
                stagedFiles.put(new File(entry.getKey()), new File(entry.getValue()));
            }

            deletedFiles.clear();

            for (String path : journal.deletedFiles) {
                deletedFiles.add(new File(path));
            }

            if (journal.committing) {
                Log.w(TAG, "Resuming interrupted commit for transaction: "
                        + getTransactionId());
                try {
                    commit();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to resume interrupted commit", e);
                    throw new FilesTransactionException("Failed to resume interrupted commit", e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load transaction journal", e);
        }
    }

    private static class Journal {
        @JsonProperty("staged_files")
        public Map<String, String> stagedFiles = new HashMap<>();
        @JsonProperty("deleted_files")
        public Set<String> deletedFiles = new HashSet<>();
        @JsonProperty("committing")
        public boolean committing = false;
    }
}
