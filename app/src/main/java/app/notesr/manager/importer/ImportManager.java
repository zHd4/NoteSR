package app.notesr.manager.importer;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import app.notesr.crypto.BackupsCrypt;
import app.notesr.exception.DecryptionFailedException;

public class ImportManager extends BaseImportManager {
    private static final String TAG = ImportManager.class.getName();

    public ImportManager(Context context, FileInputStream sourceStream) {
        super(context, sourceStream);
    }

    @Override
    public void start() {

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
}
