package com.peew.notesr.manager.importer;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.BackupsCrypt;
import com.peew.notesr.exception.DecryptionFailedException;
import com.peew.notesr.exception.ImportFailedException;
import com.peew.notesr.manager.BaseManager;
import com.peew.notesr.tools.FileWiper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ImportManager extends BaseManager {
    private static final String TAG = BaseManager.class.getName();

    private final FileInputStream sourceStream;
    private final Context context;

    private ImportResult result = ImportResult.NONE;
    private String status = "";
    private File jsonTempFile;
    private boolean transactionStarted = false;

    public ImportManager(Context context, FileInputStream sourceStream) {
        this.context = context;
        this.sourceStream = sourceStream;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            try {
                jsonTempFile = File.createTempFile("import", ".json");
                status = context.getString(R.string.decrypting_data);
                decrypt(sourceStream, getOutputStream(jsonTempFile));

                status = context.getString(R.string.importing);
                begin();

                clearTables();
                importData(jsonTempFile);

                end();

                status = context.getString(R.string.wiping_temp_data);
                wipeFile(jsonTempFile);

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (IOException e) {
                if (transactionStarted) rollback();
                throw new RuntimeException(e);
            } catch (DecryptionFailedException e) {
                if (transactionStarted) rollback();
                wipeFile(jsonTempFile);

                status = context.getString(R.string.cannot_decrypt_file);
                result = ImportResult.DECRYPTION_FAILED;
            } catch (ImportFailedException e) {
                if (transactionStarted) rollback();
                wipeFile(jsonTempFile);

                status = context.getString(R.string.cannot_import_data);
                result = ImportResult.IMPORT_FAILED;
            }
        });

        thread.start();
    }

    public ImportResult getResult() {
        return result;
    }

    public String getStatus() {
        return status;
    }

    private void decrypt(FileInputStream inputStream, FileOutputStream outputStream) throws DecryptionFailedException {
        if (result == ImportResult.NONE) {
            try {
                BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
                backupsCrypt.decrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new DecryptionFailedException();
            }
        }
    }

    private void clearTables() {
        getDataBlocksTable().deleteAll();
        getFilesInfoTable().deleteAll();
        getNotesTable().deleteAll();
    }

    private void importData(File file) throws ImportFailedException {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(file);

            try (jsonParser) {
                NotesImporter notesImporter = new NotesImporter(
                        jsonParser,
                        getNotesTable(),
                        getTimestampFormatter()
                );

                FilesImporter filesImporter = new FilesImporter(
                        jsonParser,
                        getFilesInfoTable(),
                        getDataBlocksTable(),
                        getTimestampFormatter()
                );

                notesImporter.importNotes();
                filesImporter.importFiles();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
            throw new ImportFailedException();
        }
    }

    private void wipeFile(File file) {
        if (result == ImportResult.NONE) {
            FileWiper wiper = new FileWiper(file);

            try {
                boolean success = wiper.wipeFile();

                if (!success) {
                    throw new RuntimeException("Filed to wipe file");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void begin() {
        App.getAppContainer().getNotesDB().beginTransaction();
        transactionStarted = true;
    }

    private void rollback() {
        App.getAppContainer().getNotesDB().rollbackTransaction();
        transactionStarted = false;
    }

    private void end() {
        App.getAppContainer().getNotesDB().commitTransaction();
        transactionStarted = false;
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }

    private FileOutputStream getOutputStream(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    public enum ImportResult {
        NONE, FINISHED_SUCCESSFULLY, DECRYPTION_FAILED, IMPORT_FAILED
    }
}
