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

    private static final String TAG = ExportManager.class.getName();

    private final Context context;
    private final File outputFile;

    private NotesWriter notesWriter;
    private FilesWriter filesWriter;

    private Thread jsonGeneratorThread;
    private Thread encryptorThread;
    private Thread wiperThread;

    private File jsonTempFile;
    private boolean finished = false;
    private String status = "";

    public ExportManager(Context context, File outputFile) {
        this.context = context;
        this.outputFile = outputFile;
    }

    public void export() throws InterruptedException {
        jsonGeneratorThread = new Thread(tempJsonGenerator());
        encryptorThread = new Thread(encryptor());
        wiperThread = new Thread(wiper());

        status = context.getString(R.string.exporting_data);

        jsonGeneratorThread.start();
        jsonGeneratorThread.join();

        status = context.getString(R.string.encrypting_data);

        encryptorThread.start();
        encryptorThread.join();

        status = context.getString(R.string.wiping_temp_data);

        wiperThread.start();
        wiperThread.join();

        status = "";
        finished = true;
    }

    public int calculateProgress() {
        if (notesWriter == null || filesWriter == null) {
            return 0;
        }

        if (finished) {
            return 100;
        }

        long total = notesWriter.getTotal() + filesWriter.getTotal();
        long exported = notesWriter.getExported() + filesWriter.getExported();

        return Math.round((exported * 99.0f) / total);
    }

    public String getStatus() {
        return status;
    }

    private Runnable tempJsonGenerator() {
        return () -> {
            try {
                jsonTempFile = File.createTempFile("export", ".json");

                JsonFactory jsonFactory = new JsonFactory();
                JsonGenerator jsonGenerator = jsonFactory.createGenerator(jsonTempFile, JsonEncoding.UTF8);

                notesWriter = getNotesWriter(jsonGenerator);
                filesWriter = getFilesWriter(jsonGenerator);

                try (jsonGenerator) {
                    jsonGenerator.writeStartObject();
                    writeVersion(jsonGenerator);

                    notesWriter.writeNotes();
                    filesWriter.writeFiles();

                    jsonGenerator.writeEndObject();
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException();
            }
        };
    }

    private Runnable encryptor() {
        return () -> {
            try {
                BackupsCrypt backupsCrypt = new BackupsCrypt(jsonTempFile, outputFile);
                backupsCrypt.encrypt();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        };
    }

    private Runnable wiper() {
        return () -> {
            try {
                FileWiper fileWiper = new FileWiper(jsonTempFile);
                boolean success = fileWiper.wipeFile();

                if (!success) {
                    throw new RuntimeException("Filed to wipe file");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        };
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
