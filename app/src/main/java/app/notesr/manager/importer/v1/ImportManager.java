package app.notesr.manager.importer.v1;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import app.notesr.App;
import app.notesr.R;
import app.notesr.crypto.BackupsCrypt;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.ImportFailedException;
import app.notesr.manager.BaseManager;
import app.notesr.manager.importer.ImportResult;
import app.notesr.utils.Wiper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import lombok.Getter;

public class ImportManager extends BaseManager {
    private static final String TAG = BaseManager.class.getName();

    private final FileInputStream sourceStream;
    private final Context context;

    @Getter
    private ImportResult result = ImportResult.NONE;

    @Getter
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
            throw new ImportFailedException(e);
        }
    }

    private void wipeFile(File file) {
        if (result == ImportResult.NONE) {
            try {
                boolean success = Wiper.wipeFile(file);

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
}
