package app.notesr.importer.service.v3;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import java.io.File;
import java.nio.file.Path;

import app.notesr.db.AppDatabase;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.ImportStrategy;
import app.notesr.note.service.NoteService;
import app.notesr.security.crypto.AesGcmCryptor;
import app.notesr.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportV3Strategy implements ImportStrategy {

    private final CryptoSecrets cryptoSecrets;
    private final AppDatabase db;
    private final NoteService noteService;
    private final FileService fileService;
    private final File tempDecryptedBackupFile;

    @Override
    public void execute() {
        db.runInTransaction(() -> {
            BackupDecryptor decryptor = getBackupDecryptor();
            DataImporter dataImporter = getDataImporter(decryptor);
            dataImporter.importData();
            return null;
        });
    }

    private BackupDecryptor getBackupDecryptor() {
        AesGcmCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));
        return new BackupDecryptor(cryptor);
    }

    DataImporter getDataImporter(BackupDecryptor decryptor) {
        Path backupZipPath = tempDecryptedBackupFile.toPath();
        return new DataImporter(decryptor, noteService, fileService, backupZipPath);
    }
}
