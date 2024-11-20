package app.notesr.manager.importer;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import app.notesr.R;
import app.notesr.crypto.BackupsCrypt;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.utils.Wiper;

public class ImportManager extends BaseImportManager {
    private static final String TAG = ImportManager.class.getName();

    public ImportManager(Context context, FileInputStream sourceStream) {
        super(context, sourceStream);
    }

    @Override
    public void start() {
        Thread thread = new Thread(() -> {
            File tempDecryptedFile = new File(context.getCacheDir(), randomUUID().toString());

            try {
                FileOutputStream outputStream = new FileOutputStream(tempDecryptedFile);
                decrypt(sourceStream, outputStream);
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

    private static void decrypt(FileInputStream inputStream, FileOutputStream outputStream) throws
            DecryptionFailedException {
        try {
            BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
            backupsCrypt.decrypt();
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
