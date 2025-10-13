package app.notesr.security.service;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

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

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.file.service.FileService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecretsUpdateService {
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

        AppDatabase oldDb = getDatabase(dbName, getSupportFactory(oldKey));
        AppDatabase newDb = getDatabase(newDbFile.getName(), getSupportFactory(newKey));

        AesCryptor oldCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(oldSecrets));
        AesCryptor newCryptor = new AesGcmCryptor(getSecretKeyFromSecrets(newSecrets));

        newDb.runInTransaction(() -> {
            copyDbData(oldDb, newDb);

            File blobsDir = new File(context.getFilesDir(), FileService.BLOBS_DIR_NAME);
            updateBlobsData(newDb, blobsDir, oldCryptor, newCryptor);

            return null;
        });

        oldDb.close();
        newDb.close();

        replaceDatabase(oldDbFile, newDbFile);

        cryptoManager.setSecrets(context, newSecrets);
        DatabaseProvider.reinit(context, getSupportFactory(newKey));
    }

    private void copyDbData(AppDatabase oldDb, AppDatabase newDb) {
        List<Note> allNotes = oldDb.getNoteDao().getAll();
        newDb.getNoteDao().insertAll(allNotes);

        List<FileInfo> allFilesInfo = oldDb.getFileInfoDao().getAll();
        newDb.getFileInfoDao().insertAll(allFilesInfo);

        List<FileBlobInfo> allFilesBlobInfo = oldDb.getFileBlobInfoDao().getAll();
        newDb.getFileBlobInfoDao().insertAll(allFilesBlobInfo);
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

    private AppDatabase getDatabase(String name, SupportFactory factory) {
        return Room.databaseBuilder(context, AppDatabase.class, name)
                .openHelperFactory(factory)
                .build();
    }
}
