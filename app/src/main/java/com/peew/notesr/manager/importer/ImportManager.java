package com.peew.notesr.manager.importer;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.BackupsCrypt;
import com.peew.notesr.manager.BaseManager;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ImportManager extends BaseManager {
    private static final String TAG = BaseManager.class.getName();
    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;
    public static final int CANCELED = -1;

    private final File file;
    private final Context context;

    private int result = NONE;
    private String status = "";
    private Thread thread;
    private File jsonTempFile;

    public ImportManager(Context context, File file) {
        this.context = context;
        this.file = file;
    }

    public void start() {
        thread = new Thread(() -> {
            try {
                jsonTempFile = File.createTempFile("import", ".json");
                status = context.getString(R.string.decrypting_data);

                decrypt(file, jsonTempFile);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }

    public int getResult() {
        return result;
    }

    public String getStatus() {
        return status;
    }

    private void decrypt(File input, File output) {
        if (result == NONE) {
            try {
                BackupsCrypt backupsCrypt = new BackupsCrypt(input, output);
                backupsCrypt.decrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importData(File file) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(file);

        NotesImporter notesImporter = new NotesImporter(
                jsonParser,
                getNotesTable(),
                getTimestampFormatter()
        );

        try (jsonParser) {
            notesImporter.importNotes();
        }
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
