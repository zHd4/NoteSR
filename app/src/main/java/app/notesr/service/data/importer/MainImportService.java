package app.notesr.service.data.importer;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import app.notesr.App;
import app.notesr.R;
import app.notesr.crypto.BackupCryptor;
import app.notesr.db.notes.NotesDb;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.service.data.importer.v1.ImportServiceV1;
import app.notesr.service.data.importer.v2.ImportServiceV2;
import app.notesr.util.Wiper;
import app.notesr.util.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainImportService {

    private static final String TAG = MainImportService.class.getName();

    private final Context context;
    private final FileInputStream sourceStream;

    private ImportResult result = ImportResult.NONE;
    private String status = "";

    private ImportServiceBase usingService;

    public void start() {
        Thread thread = new Thread(() -> {
            status = context.getString(R.string.decrypting_data);
            File tempDecryptedFile = new File(context.getCacheDir(), randomUUID().toString());

            try {
                FileOutputStream outputStream = new FileOutputStream(tempDecryptedFile);
                decrypt(sourceStream, outputStream);

                NotesDb notesDb = App.getAppContainer().getNotesDB();

                if (ZipUtils.isZipArchive(tempDecryptedFile.getAbsolutePath())) {
                    usingService = new ImportServiceV2(context, notesDb, tempDecryptedFile);
                } else {
                    usingService = new ImportServiceV1(context, notesDb, tempDecryptedFile);
                }

                usingService.start();
            } catch (DecryptionFailedException e) {
                if (tempDecryptedFile.exists()) {
                    wipeFile(tempDecryptedFile);
                }

                status = context.getString(R.string.cannot_decrypt_file);
                result = ImportResult.DECRYPTION_FAILED;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }

    public ImportResult getResult() {
        if (usingService != null) {
            return usingService.getResult();
        }

        return result;
    }

    public String getStatus() {
        if (usingService != null) {
            return usingService.getStatus();
        }

        return status;
    }

    private static void decrypt(FileInputStream inputStream, FileOutputStream outputStream) throws
            DecryptionFailedException {
        try {
            BackupCryptor backupCryptor = new BackupCryptor(inputStream, outputStream);
            backupCryptor.decrypt();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new DecryptionFailedException();
        }
    }

    private static void wipeFile(File file) {
        try {
            if (!Wiper.wipeFile(file)) {
                throw new RuntimeException("Cannot delete file: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
