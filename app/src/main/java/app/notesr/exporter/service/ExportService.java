package app.notesr.exporter.service;

import static java.util.UUID.randomUUID;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.notesr.exception.EncryptionFailedException;
import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.db.AppDatabase;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.TempDataWiper;
import app.notesr.util.VersionFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportService {
    private static final String TAG = ExportService.class.getName();

    private final Context context;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File outputFile;
    private final ExportStatusHolder statusHolder;
    private final CryptoSecrets cryptoSecrets;

    private File tempArchive;
    private int exportedEntities;

    public void doExport() {
        if (db.getNoteDao().getRowsCount() == 0) {
            throw new DataNotFoundException("No notes in table");
        }

        try {
            tempArchive = new File(context.getCacheDir(), randomUUID().toString() + ".zip");

            statusHolder.setStatus(ExportStatus.INITIALIZING);
            BackupEncryptor backupEncryptor = getBackupEncryptor();

            try (BackupZipper zipper = new BackupZipper(tempArchive)) {
                statusHolder.setStatus(ExportStatus.EXPORTING_DATA);

                exportVersion(zipper);
                exportNotes(zipper, backupEncryptor);
                exportFilesInfos(zipper, backupEncryptor);
                exportDataBlocks(zipper, backupEncryptor);
            }

            statusHolder.setStatus(ExportStatus.ENCRYPTING_DATA);
            encryptFinalFile(backupEncryptor);
            deleteFile(tempArchive);

            statusHolder.setStatus(ExportStatus.DONE);
            statusHolder.setProgress(calculateProgress());
        } catch (ExportCancelledException e) {
            statusHolder.setStatus(ExportStatus.CANCELED);
        } catch (Throwable e) {
            Log.e(TAG, "Export failed", e);

            try {
                deleteFile(tempArchive);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            statusHolder.setStatus(ExportStatus.ERROR);
        }
    }

    public void cancel() {
        statusHolder.setStatus(ExportStatus.CANCELLING);
    }

    private void exportVersion(BackupZipper zipper)
            throws PackageManager.NameNotFoundException, IOException {

        zipper.addVersionFile(VersionFetcher.fetchVersionName(context, false));
    }

    private void exportNotes(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        for (Note note : noteService.getAll()) {
            checkCancelled();

            String json = getObjectMapper().writeValueAsString(note);
            byte[] encryptedJson = encryptor.encrypt(json);

            zipper.addNote(note.getId(), encryptedJson);
            increaseProgress();
        }
    }

    private void exportFilesInfos(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        for (FileInfo fileInfo : fileService.getAllFilesInfo()) {
            checkCancelled();

            String json = getObjectMapper().writeValueAsString(fileInfo);
            byte[] encryptedJson = encryptor.encrypt(json);

            zipper.addFileInfo(fileInfo.getId(), encryptedJson);
            increaseProgress();
        }
    }

    private void exportDataBlocks(BackupZipper zipper, BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        for (DataBlock blockWithoutData : fileService.getAllDataBlocksWithoutData()) {
            checkCancelled();

            DataBlock dataBlock = fileService.getDataBlock(blockWithoutData.getId());

            String json = getObjectMapper().writeValueAsString(dataBlock);
            byte[] encryptedJson = encryptor.encrypt(json);

            zipper.addDataBlock(dataBlock.getId(), encryptedJson);
            increaseProgress();
        }
    }

    private void encryptFinalFile(BackupEncryptor encryptor)
            throws IOException, EncryptionFailedException {

        FileInputStream inputStream = new FileInputStream(tempArchive);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        encryptor.encrypt(inputStream, outputStream);
    }

    private BackupEncryptor getBackupEncryptor() {
        AesGcmCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));
        return new BackupEncryptor(cryptor);
    }

    private void checkCancelled() {
        ExportStatus status = statusHolder.getStatus();

        if (status == ExportStatus.CANCELLING) {
            try {
                TempDataWiper.wipeTempData(tempArchive);

                if (outputFile.exists()) {
                    Files.delete(outputFile.toPath());
                }

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
        ExportStatus status = statusHolder.getStatus();

        if (status == null || status == ExportStatus.INITIALIZING) {
            return 0;
        } else if (status == ExportStatus.DONE) {
            return 100;
        }

        long notesCount = noteService.getCount();
        long filesCount = fileService.getFilesCount();
        long dataBlocksCount = fileService.getDataBlocksCount();

        long total = notesCount + filesCount + dataBlocksCount;

        return Math.round((exportedEntities * 99.0f) / total);
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
