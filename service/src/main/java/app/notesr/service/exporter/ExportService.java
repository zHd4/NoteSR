/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.exporter;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.TempDataWiper;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;

import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExportService {

    private static final String TAG = ExportService.class.getCanonicalName();

    private final CryptoSecrets cryptoSecrets;
    private final String appVersion;

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;

    private final ExportStatusHolder statusHolder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private File tempArchive;
    private int exportedEntities;

    public void doExport(OutputStream outputStream) {
        if (db.getNoteDao().getRowsCount() == 0) {
            throw new DataNotFoundException("No notes in table");
        }

        try {
            tempArchive = new File(context.getCacheDir(), randomUUID().toString() + ".zip");

            statusHolder.setStatus(ExportStatus.INITIALIZING);
            var backupEncryptor = getBackupEncryptor();

            try (var zipper = new BackupZipper(tempArchive)) {
                statusHolder.setStatus(ExportStatus.EXPORTING_DATA);

                exportVersion(zipper);
                exportNotes(zipper, backupEncryptor);
                exportFilesInfo(zipper, backupEncryptor);
                exportFilesData(zipper, backupEncryptor);
            }

            statusHolder.setStatus(ExportStatus.ENCRYPTING_DATA);
            encryptFinalFile(backupEncryptor, outputStream);

            statusHolder.setStatus(ExportStatus.DONE);
            statusHolder.setProgress(calculateProgress());
        } catch (ExportCancelledException e) {
            statusHolder.setStatus(ExportStatus.CANCELED);
        } catch (Throwable e) {
            Log.e(TAG, "Export failed", e);
            statusHolder.setStatus(ExportStatus.ERROR);
        } finally {
            try {
                deleteFile(tempArchive);
            } catch (IOException e) {
                Log.e(TAG, "Failed to delete temp archive", e);
            }
        }
    }

    public void cancel() {
        statusHolder.setStatus(ExportStatus.CANCELLING);
    }

    private void exportVersion(BackupZipper zipper) throws IOException {
        zipper.addVersionFile(appVersion);
    }

    private void exportNotes(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        for (var note : noteService.getAll()) {
            checkCancelled();

            var json = objectMapper.writeValueAsString(note);
            byte[] encryptedJson = encryptor.encrypt(json);

            zipper.addNote(note.getId(), encryptedJson);
            increaseProgress();
        }
    }

    private void exportFilesInfo(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        for (FileInfo fileInfo : fileService.getFilesInfo()) {
            checkCancelled();

            var json = objectMapper.writeValueAsString(fileInfo);
            byte[] encryptedJson = encryptor.encrypt(json);

            zipper.addFileInfo(fileInfo.getId(), encryptedJson);
            increaseProgress();
        }
    }

    private void exportFilesData(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException, DecryptionFailedException {

        for (FileBlobInfo blobInfo : fileService.getFilesBlobInfo()) {
            checkCancelled();

            var blobInfoJson = objectMapper.writeValueAsString(blobInfo);
            byte[] blobData = fileService.getBlobData(blobInfo.getId());

            byte[] encryptedBlobInfo = encryptor.encrypt(blobInfoJson);
            byte[] encryptedBlobData = encryptor.encrypt(blobData);

            zipper.addBlob(blobInfo.getId(), encryptedBlobInfo, encryptedBlobData);
            increaseProgress();
        }
    }

    private void encryptFinalFile(BackupEncryptor encryptor, OutputStream outputStream)
            throws IOException, EncryptionFailedException {

        try (var fileInputStream = new FileInputStream(tempArchive)) {
            encryptor.encrypt(fileInputStream, outputStream);
        }
    }

    private BackupEncryptor getBackupEncryptor() {
        return new BackupEncryptor(AesCryptorFactory.createAesGcmCryptor(cryptoSecrets));
    }

    private void checkCancelled() {
        var status = statusHolder.getStatus();

        if (status == ExportStatus.CANCELLING) {
            try {
                TempDataWiper.wipeTempData(tempArchive);

                throw new ExportCancelledException();
            } catch (IOException e) {
                throw new ExportCancelledException(e);
            }
        }
    }

    private void deleteFile(File file) throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + file.getAbsolutePath());
            }
        }
    }

    private void increaseProgress() {
        exportedEntities++;
        statusHolder.setProgress(calculateProgress());
    }

    private int calculateProgress() {
        var status = statusHolder.getStatus();

        if (status == null || status == ExportStatus.INITIALIZING) {
            return 0;
        } else if (status == ExportStatus.DONE) {
            return 100;
        }

        long notesCount = noteService.getCount();
        long filesCount = fileService.getFilesCount();
        long dataBlocksCount = fileService.getBlobsCount();

        long total = notesCount + filesCount + dataBlocksCount;

        return Math.round((exportedEntities * 99.0f) / total);
    }
}
