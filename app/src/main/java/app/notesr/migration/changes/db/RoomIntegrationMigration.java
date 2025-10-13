package app.notesr.migration.changes.db;

import static app.notesr.core.util.KeyUtils.getIvFromSecrets;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import app.notesr.core.security.crypto.AesCbcCryptor;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
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
import app.notesr.file.service.FileService;
import app.notesr.migration.service.AppMigration;
import app.notesr.migration.service.AppMigrationException;
import app.notesr.note.service.NoteService;
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

            filesUtils = getFilesUtils();
            noteService = getNoteService(db);
            fileService = getFileService(context, db, filesUtils);
            oldDbHelper = getOldDbHelper(context);

            CryptoSecrets cryptoSecrets = getCryptoSecrets(context);
            entityMapper = getMapper(cryptoSecrets);

            wiper = getWiper();

            db.runInTransaction(() -> {
                migrateNotes();
                migrateFiles();
                wipeOldDbs(context);
            });
        } catch (Throwable e) {
            throw new AppMigrationException("Unhandled migration exception", e);
        }
    }

    private void migrateNotes() {
        for (Map<String, Object> noteMap : oldDbHelper.getAllNotes()) {
            try {
                noteService.importNote(entityMapper.mapNote(noteMap));
            } catch (DecryptionFailedException e) {
                throw new AppMigrationException("Failed to decrypt note with id "
                        + noteMap.get("id"), e);
            }
        }
    }

    private void migrateFiles() {
        for (Map<String, Object> fileInfoMap : oldDbHelper.getFilesInfo()) {
            FileInfo fileInfo;

            try {
                fileInfo = entityMapper.mapFileInfo(fileInfoMap);
            } catch (DecryptionFailedException e) {
                throw new AppMigrationException("Failed to decrypt file info with id "
                        + fileInfoMap.get("id"), e);
            }

            fileService.importFileInfo(fileInfo);

            for (String dataBlockId : oldDbHelper.getBlocksIdsByFileId(fileInfo.getId())) {
                Map<String, Object> dataBlockMap = oldDbHelper.getDataBlockById(dataBlockId);

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
            File file = filesUtils.getDatabaseFile(context, filePath);

            try {
                wiper.wipeFile(file);
            } catch (IOException e) {
                throw new AppMigrationException("Failed to wipe file " + file.getPath(), e);
            }
        });
    }

    CryptoSecrets getCryptoSecrets(Context context) {
        return CryptoManagerProvider.getInstance(context).getSecrets();
    }

    AppDatabase getAppDatabase(Context context) {
        return DatabaseProvider.getInstance(context);
    }

    NoteService getNoteService(AppDatabase db) {
        return new NoteService(db);
    }

    FileService getFileService(Context context, AppDatabase db, FilesUtilsAdapter filesUtils) {
        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        return new FileService(context, db, cryptor, filesUtils);
    }

    OldDbHelper getOldDbHelper(Context context) {
        return new OldDbHelper(context);
    }

    EntityMapper getMapper(CryptoSecrets cryptoSecrets) {
        SecretKey key = getSecretKeyFromSecrets(cryptoSecrets);
        byte[] iv = getIvFromSecrets(cryptoSecrets);

        AesCryptor aesCryptor = new AesCbcCryptor(key, iv);
        ValueDecryptor valueDecryptor = new ValueDecryptor(aesCryptor);

        return new EntityMapper(valueDecryptor);
    }

    FilesUtilsAdapter getFilesUtils() {
        return new FilesUtils();
    }

    WiperAdapter getWiper() {
        return new Wiper();
    }
}
