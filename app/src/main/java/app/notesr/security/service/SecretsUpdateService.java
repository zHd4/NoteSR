package app.notesr.security.service;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SupportFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import app.notesr.db.DatabaseProvider;
import app.notesr.db.AppDatabase;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtilsAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretsUpdateService {
    private final Context context;
    private final String dbName;
    private final CryptoManager cryptoManager;
    private final CryptoSecrets newSecrets;
    private final FilesUtilsAdapter filesUtils;

    public void update() throws EncryptionFailedException, DecryptionFailedException, IOException {
        CryptoSecrets oldSecrets = cryptoManager.getSecrets();

        byte[] oldKey = oldSecrets.getKey();
        byte[] newKey = newSecrets.getKey();

        File oldDbFile = context.getDatabasePath(dbName);
        File newDbFile = context.getDatabasePath("tmp_" + dbName);

        DatabaseProvider.close();

        AppDatabase oldDb = getDatabase(context, dbName, getSupportFactory(oldKey));
        AppDatabase newDb = getDatabase(context, newDbFile.getName(), getSupportFactory(newKey));

        AesCryptor oldCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(oldSecrets));
        AesCryptor newCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(newSecrets));

        moveDataInTransaction(oldDb, newDb);
        updateBlobsData(newDb, new File(context.getFilesDir(), FileService.BLOBS_DIR_NAME),
                oldCryptor, newCryptor);

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

            List<FileInfo> allFilesInfo = oldDb.getFileInfoDao().getAll();
            newDb.getFileInfoDao().insertAll(allFilesInfo);

            List<FileBlobInfo> allFilesBlobInfo = oldDb.getFileBlobInfoDao().getAll();
            newDb.getFileBlobInfoDao().insertAll(allFilesBlobInfo);
        });
    }

    private void updateBlobsData(AppDatabase db,
                                 File blobsDir,
                                 AesCryptor oldCryptor,
                                 AesCryptor newCryptor)
            throws IOException, EncryptionFailedException, DecryptionFailedException {

        List<FileBlobInfo> blobsInfo = db.getFileBlobInfoDao().getAll();

        for (FileBlobInfo blobInfo : blobsInfo) {
            File blobFile = new File(blobsDir, blobInfo.getId());
            byte[] data = filesUtils.readFileBytes(blobFile);

            try {
                data = oldCryptor.decrypt(data);
            } catch (GeneralSecurityException e) {
                throw new DecryptionFailedException(e);
            }

            try {
                data = newCryptor.encrypt(data);
            } catch (GeneralSecurityException e) {
                throw new EncryptionFailedException(e);
            }

            filesUtils.writeFileBytes(blobFile, data);
        }
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
