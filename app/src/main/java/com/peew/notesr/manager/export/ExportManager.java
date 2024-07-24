package com.peew.notesr.manager.export;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.BackupsCrypt;
import com.peew.notesr.manager.BaseManager;
import com.peew.notesr.tools.FileWiper;
import com.peew.notesr.tools.VersionFetcher;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ExportManager extends BaseManager {

    public static final int NONE = 0;
    public static final int FINISHED_SUCCESSFULLY = 2;
    public static final int CANCELED = -1;
    private static final String TAG = ExportManager.class.getName();

    private final Context context;
    private final File outputFile;

    private Thread thread;

    private NotesWriter notesWriter;
    private FilesWriter filesWriter;

    private File jsonTempFile;
    private int result = NONE;
    private String status = "";

    public ExportManager(Context context, File outputFile) {
        this.context = context;
        this.outputFile = outputFile;
    }

    public void start() {
        thread = new Thread(() -> {
            try {
                status = context.getString(R.string.exporting_data);
                jsonTempFile = File.createTempFile("export", ".json");

                generateJson(jsonTempFile);

                status = context.getString(R.string.encrypting_data);
                encrypt(jsonTempFile, outputFile);

                status = context.getString(R.string.wiping_temp_data);
                wipe(jsonTempFile);

                status = "";
                result = FINISHED_SUCCESSFULLY;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }

    public void cancel() {
        if (thread == null || !thread.isAlive()) {
            throw new IllegalStateException("Export has not been started");
        }

        status = context.getString(R.string.canceling);
        thread.interrupt();

        if (jsonTempFile.exists()) {
            wipe(jsonTempFile);
        }

        if (outputFile.exists()) {
            delete(outputFile);
        }

        status = "";
        result = CANCELED;
    }

    public int calculateProgress() {
        if (notesWriter == null || filesWriter == null) {
            return 0;
        }

        if (result == FINISHED_SUCCESSFULLY) {
            return 100;
        }

        long total = notesWriter.getTotal() + filesWriter.getTotal();
        long exported = notesWriter.getExported() + filesWriter.getExported();

        return Math.round((exported * 99.0f) / total);
    }

    public String getStatus() {
        return status;
    }

    public int getResult() {
        return result;
    }

    private void generateJson(File output) {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(output, JsonEncoding.UTF8);

            notesWriter = getNotesWriter(jsonGenerator);
            filesWriter = getFilesWriter(jsonGenerator);

            if (result == NONE) {
                try (jsonGenerator) {
                    jsonGenerator.writeStartObject();
                    writeVersion(jsonGenerator);

                    notesWriter.writeNotes();
                    filesWriter.writeFiles();

                    jsonGenerator.writeEndObject();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    private void encrypt(File input, File output) {
        if (result == NONE) {
            try {
                BackupsCrypt backupsCrypt = new BackupsCrypt(input, output);
                backupsCrypt.encrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void wipe(File file) {
        if (result == NONE) {
            try {
                FileWiper fileWiper = new FileWiper(file);
                boolean success = fileWiper.wipeFile();

                if (!success) {
                    throw new RuntimeException("Filed to wipe file");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void delete(File file) {
        if (!file.delete()) {
            throw new RuntimeException("Cannot delete file " + file.getAbsolutePath());
        }
    }

    private void writeVersion(JsonGenerator jsonGenerator) throws IOException {
        try {
            String version = VersionFetcher.fetchVersionName(context, false);
            jsonGenerator.writeStringField("version", version);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private NotesWriter getNotesWriter(JsonGenerator jsonGenerator) {
        return new NotesWriter(
                jsonGenerator,
                getNotesTable(),
                getTimestampFormatter()
        );
    }

    private FilesWriter getFilesWriter(JsonGenerator jsonGenerator) {
        return new FilesWriter(
                jsonGenerator,
                getFilesInfoTable(),
                getDataBlocksTable(),
                getTimestampFormatter()
        );
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
