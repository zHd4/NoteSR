package app.notesr.service.data.importer;

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
import app.notesr.service.ServiceBase;
import app.notesr.service.data.importer.v1.ImportServiceV1;
import app.notesr.service.data.importer.v2.ImportServiceV2;
import app.notesr.utils.Wiper;
import app.notesr.utils.ZipUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainImportService extends ServiceBase {

    private static final String TAG = MainImportService.class.getName();

    private final Context context;
    private final FileInputStream sourceStream;

    private ImportResult result = ImportResult.NONE;
    private String status = "";

    private ImportServiceBase usingManager;

    public void start() {
        Thread thread = new Thread(() -> {
            status = context.getString(R.string.decrypting_data);
            File tempDecryptedFile = new File(context.getCacheDir(), randomUUID().toString());

            try {
                FileOutputStream outputStream = new FileOutputStream(tempDecryptedFile);
                decrypt(sourceStream, outputStream);

                if (ZipUtils.isZipArchive(tempDecryptedFile.getAbsolutePath())) {
                    usingManager = new ImportServiceV2(context, tempDecryptedFile);
                } else {
                    usingManager = new ImportServiceV1(context, tempDecryptedFile);
                }

                usingManager.start();
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
        if (usingManager != null) {
            return usingManager.getResult();
        }

        return result;
    }

    public String getStatus() {
        if (usingManager != null) {
            return usingManager.getStatus();
        }

        return status;
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
