package app.notesr.security.service;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SupportFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import app.notesr.db.DatabaseProvider;
import app.notesr.db.AppDatabase;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretsUpdateService {
    private final Context context;
    private final String dbName;
    private final CryptoManager cryptoManager;
    private final CryptoSecrets newSecrets;

    public void update() throws EncryptionFailedException, IOException {
        byte[] oldKey = cryptoManager.getSecrets().getKey();
        byte[] newKey = newSecrets.getKey();

        File oldDbFile = context.getDatabasePath(dbName);
        File newDbFile = context.getDatabasePath("tmp_" + dbName);

        DatabaseProvider.close();

        AppDatabase oldDb = getDatabase(context, dbName, getSupportFactory(oldKey));
        AppDatabase newDb = getDatabase(context, newDbFile.getName(), getSupportFactory(newKey));

        moveDataInTransaction(oldDb, newDb);

        oldDb.close();
        newDb.close();

        replaceDatabase(oldDbFile, newDbFile);
        cryptoManager.setSecrets(context, newSecrets);
        DatabaseProvider.reinit(context, getSupportFactory(newKey));
    }

    private void moveDataInTransaction(AppDatabase oldDb, AppDatabase newDb) {
        newDb.runInTransaction(() -> {
            List<Note> allNotes = oldDb.getNoteDao().getAll();
            newDb.getNoteDao().insertAll(allNotes);

            List<FileInfo> allFiles = oldDb.getFileInfoDao().getAll();
            newDb.getFileInfoDao().insertAll(allFiles);

            oldDb.getDataBlockDao().getAllWithoutData().forEach(block -> {
                block.setData(oldDb.getDataBlockDao().get(block.getId()).getData());
                newDb.getDataBlockDao().insert(block);
            });
        });
    }

    private void replaceDatabase(File oldDbFile, File newDbFile) throws IOException {
        String oldDbPath = oldDbFile.getAbsolutePath();
        List<String> filesToDelete = List.of(
                oldDbPath,
                oldDbPath + "-shm",
                oldDbPath + "-wal"
        );

        for (String path : filesToDelete) {
            Files.deleteIfExists(Paths.get(path));
        }

        Files.move(newDbFile.toPath(), oldDbFile.toPath());
    }

    private SupportFactory getSupportFactory(byte[] key) {
        return new SupportFactory(Arrays.copyOf(key, key.length));
    }

    private AppDatabase getDatabase(Context context, String name, SupportFactory factory) {
        return Room.databaseBuilder(context, AppDatabase.class, name)
                .openHelperFactory(factory)
                .build();
    }
}
