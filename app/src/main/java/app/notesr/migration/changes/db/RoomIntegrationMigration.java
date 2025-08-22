package app.notesr.migration.changes.db;

import static app.notesr.util.KeyUtils.getIvFromSecrets;
import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import java.util.Map;

import javax.crypto.SecretKey;

import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.migration.service.AppMigration;
import app.notesr.migration.service.AppMigrationException;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.AesCbcCryptor;
import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.security.crypto.ValueDecryptor;
import app.notesr.security.dto.CryptoSecrets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoomIntegrationMigration implements AppMigration {

    @Getter
    private final int fromVersion;

    @Getter
    private final int toVersion;

    private NoteService noteService;
    private FileService fileService;
    private OldDbHelper oldDbHelper;
    private EntityMapper entityMapper;

    @Override
    public void migrate(Context context) {
        try {
            AppDatabase db = getAppDatabase(context);

            noteService = getNoteService(db);
            fileService = getFileService(db);

            oldDbHelper = getOldDbHelper(context);

            CryptoSecrets cryptoSecrets = getCryptoSecrets();
            entityMapper = getMapper(cryptoSecrets);

            db.runInTransaction(() -> {
                migrateNotes();
                migrateFiles();
            });
        } catch (Exception e) {
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
                DataBlock dataBlock;

                try {
                    dataBlock = entityMapper.mapDataBlock(dataBlockMap);
                } catch (DecryptionFailedException e) {
                    throw new AppMigrationException("Failed to decrypt data block with id "
                            + dataBlockMap.get("id"), e);
                }

                fileService.importDataBlock(dataBlock);
            }
        }
    }

    CryptoSecrets getCryptoSecrets() {
        return CryptoManagerProvider.getInstance().getSecrets();
    }

    AppDatabase getAppDatabase(Context context) {
        return DatabaseProvider.getInstance(context);
    }

    NoteService getNoteService(AppDatabase db) {
        return new NoteService(db);
    }

    FileService getFileService(AppDatabase db) {
        return new FileService(db);
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
}
