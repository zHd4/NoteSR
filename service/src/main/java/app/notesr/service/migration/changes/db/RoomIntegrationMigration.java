/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration.changes.db;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.crypto.ValueDecryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.FilesUtils;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.Wiper;
import app.notesr.core.util.WiperAdapter;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;
import app.notesr.service.migration.AppMigration;
import app.notesr.service.migration.AppMigrationException;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RoomIntegrationMigration implements AppMigration {

    private static final List<String> FILES_TO_WIPE = List.of(
            "notes_db5",
            "notes_db5-journal",
            "services_db5",
            "services_db5-journal"
    );

    @Getter
    private final int fromVersion;

    @Getter
    private final int toVersion;

    private NoteService noteService;
    private FileService fileService;
    private OldDbHelper oldDbHelper;
    private EntityMapper entityMapper;
    private FilesUtilsAdapter filesUtils;
    private WiperAdapter wiper;

    @Override
    public void migrate(Context context) {
        try {
            AppDatabase db = getAppDatabase(context);
            CryptoSecrets cryptoSecrets = getCryptoSecrets(context);

            filesUtils = getFilesUtils();
            noteService = getNoteService(db);
            fileService = getFileService(context, cryptoSecrets, db, filesUtils);
            oldDbHelper = getOldDbHelper(context);
            entityMapper = getMapper(cryptoSecrets);
            wiper = getWiper();

            db.runInTransaction(() -> {
                migrateNotes();
                migrateFiles();
                wipeOldDbs(context);
            });

            cryptoSecrets.destroy();
        } catch (Throwable e) {
            throw new AppMigrationException("Unhandled migration exception", e);
        }
    }

    private void migrateNotes() {
        for (var noteMap : oldDbHelper.getAllNotes()) {
            try {
                noteService.importNote(entityMapper.mapNote(noteMap));
            } catch (DecryptionFailedException e) {
                throw new AppMigrationException("Failed to decrypt note with id "
                        + noteMap.get("id"), e);
            }
        }
    }

    private void migrateFiles() {
        for (var fileInfoMap : oldDbHelper.getFilesInfo()) {
            FileInfo fileInfo;

            try {
                fileInfo = entityMapper.mapFileInfo(fileInfoMap);
            } catch (DecryptionFailedException e) {
                throw new AppMigrationException("Failed to decrypt file info with id "
                        + fileInfoMap.get("id"), e);
            }

            fileService.importFileInfo(fileInfo);

            for (var dataBlockId : oldDbHelper.getBlocksIdsByFileId(fileInfo.getId())) {
                var dataBlockMap = oldDbHelper.getDataBlockById(dataBlockId);

                FileBlobInfo fileBlobInfo;
                byte[] fileBlobBytes;

                try {
                    fileBlobInfo = entityMapper.mapFileBlobInfo(dataBlockMap);
                    fileBlobBytes = entityMapper.getDataOfDataBlock(dataBlockMap);
                } catch (DecryptionFailedException e) {
                    throw new AppMigrationException("Failed to decrypt data block with id "
                            + dataBlockMap.get("id"), e);
                }

                fileService.importFileBlobInfo(fileBlobInfo);

                try {
                    fileService.importFileBlobData(fileBlobInfo.getId(), fileBlobBytes);
                } catch (IOException | EncryptionFailedException e) {
                    throw new AppMigrationException("Failed to save data block with id "
                            + dataBlockMap.get("id"), e);
                }
            }
        }
    }

    private void wipeOldDbs(Context context) {
        FILES_TO_WIPE.forEach(filePath -> {
            var file = filesUtils.getDatabaseFile(context, filePath);

            try {
                wiper.wipeFile(file);
            } catch (IOException e) {
                throw new AppMigrationException("Failed to wipe file " + file.getPath(), e);
            }
        });
    }

    AppDatabase getAppDatabase(Context context) {
        return DatabaseProvider.getInstance(context);
    }

    CryptoSecrets getCryptoSecrets(Context context) {
        return new AppSecurityService(context).getActualSecrets();
    }

    FilesUtilsAdapter getFilesUtils() {
        return new FilesUtils();
    }

    NoteService getNoteService(AppDatabase db) {
        return new NoteService(db);
    }

    FileService getFileService(
            Context context,
            CryptoSecrets cryptoSecrets,
            AppDatabase db,
            FilesUtilsAdapter filesUtils) {

        var cryptor = AesCryptorFactory.createAesGcmCryptor(cryptoSecrets);
        return new FileService(context, db, cryptor, filesUtils);
    }

    OldDbHelper getOldDbHelper(Context context) {
        return new OldDbHelper(context);
    }

    EntityMapper getMapper(CryptoSecrets cryptoSecrets) {
        var cryptor = AesCryptorFactory.createAesCbcCryptor(cryptoSecrets);
        var valueDecryptor = new ValueDecryptor(cryptor);

        return new EntityMapper(valueDecryptor);
    }

    WiperAdapter getWiper() {
        return new Wiper();
    }
}
