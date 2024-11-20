package app.notesr.manager.importer.v1;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import app.notesr.R;
import app.notesr.exception.ImportFailedException;
import app.notesr.manager.importer.BaseImportManager;
import app.notesr.manager.importer.ImportResult;
import app.notesr.utils.Wiper;

import java.io.File;
import java.io.IOException;

import lombok.Getter;

@Getter
public class ImportManagerV1 extends BaseImportManager {
    private static final String TAG = ImportManagerV1.class.getName();

    private ImportResult result = ImportResult.NONE;
    private String status = "";

    public ImportManagerV1(Context context, File file) {
        super(context, file);
    }

    @Override
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                status = context.getString(R.string.importing);
                begin();

                clearTables();
                importData(file);

                end();

                status = context.getString(R.string.wiping_temp_data);
                wipeFile(file);

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (ImportFailedException e) {
                if (isTransactionStarted()) {
                    rollback();
                }

                wipeFile(file);

                status = context.getString(R.string.cannot_import_data);
                result = ImportResult.IMPORT_FAILED;
            }
        });

        thread.start();
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
}
