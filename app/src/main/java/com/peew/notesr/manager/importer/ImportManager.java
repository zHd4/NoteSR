package com.peew.notesr.manager.importer;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.BackupsCrypt;
import com.peew.notesr.exception.InvalidDumpFormatException;
import com.peew.notesr.manager.BaseManager;
import com.peew.notesr.tools.FileWiper;

import java.io.*;
import java.time.format.DateTimeFormatter;

public class ImportManager extends BaseManager {
    private static final String TAG = BaseManager.class.getName();
    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;

    private final FileInputStream sourceStream;
    private final Context context;

    private int result = NONE;
    private String status = "";
    private File jsonTempFile;

    public ImportManager(Context context, FileInputStream sourceStream) {
        this.context = context;
        this.sourceStream = sourceStream;
    }

    public void start() throws InvalidDumpFormatException {
        Thread thread = new Thread(() -> {
            try {
                jsonTempFile = File.createTempFile("import", ".json");
                status = context.getString(R.string.decrypting_data);
                decrypt(sourceStream, getOutputStream(jsonTempFile));

                status = context.getString(R.string.importing);
                importData(jsonTempFile);

                status = context.getString(R.string.wiping_temp_data);
                wipeFile(jsonTempFile);

                status = "";
                result = FINISHED_SUCCESSFULLY;
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

    private void decrypt(FileInputStream inputStream, FileOutputStream outputStream) {
        if (result == NONE) {
            try {
                BackupsCrypt backupsCrypt = new BackupsCrypt(inputStream, outputStream);
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

        jsonParser.nextToken();
        jsonParser.nextToken();

        if (!jsonParser.getCurrentName().equals("version")) {
            InvalidDumpFormatException e = new InvalidDumpFormatException("'version' field not found");

            Log.e(TAG, "InvalidDumpFormatException", e);
            throw e;
        }

        NotesImporter notesImporter = new NotesImporter(
                jsonParser,
                getNotesTable(),
                getTimestampFormatter()
        );

        try (jsonParser) {
            notesImporter.importNotes();
        }
    }

    private void wipeFile(File file) throws IOException {
        if (result == NONE) {
            FileWiper wiper = new FileWiper(file);
            boolean success = wiper.wipeFile();

            if (!success) {
                throw new RuntimeException("Filed to wipe file");
            }
        }
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }

    private FileOutputStream getOutputStream(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }
}
