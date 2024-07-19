package com.peew.notesr.manager.export;

import android.content.Context;
import android.content.pm.PackageManager;
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

    private final Context context;

    private NotesWriter notesWriter;
    private FilesWriter filesWriter;
    private boolean finished = false;
    private String status = "";

    public ExportManager(Context context) {
        this.context = context;
    }

    public void export(File outputFile) throws IOException {
        status = context.getString(R.string.exporting_data);
        File jsonTempFile = generateTempJson();

        status = context.getString(R.string.encrypting_data);
        encrypt(jsonTempFile, outputFile);

        status = context.getString(R.string.wiping_temp_data);
        wipe(jsonTempFile);

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

    private File generateTempJson() throws IOException {
        File file = File.createTempFile("export", ".json");

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(file, JsonEncoding.UTF8);

        notesWriter = new NotesWriter(
                jsonGenerator,
                getNotesTable(),
                getTimestampFormatter()
        );

        filesWriter = new FilesWriter(
                jsonGenerator,
                getFilesInfoTable(),
                getDataBlocksTable(),
                getTimestampFormatter()
        );

        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            writeVersion(jsonGenerator);

            notesWriter.writeNotes();
            filesWriter.writeFiles();

            jsonGenerator.writeEndObject();
        }

        return file;
    }

    private void encrypt(File jsonFile, File outputFile) throws IOException {
        BackupsCrypt backupsCrypt = new BackupsCrypt(jsonFile, outputFile);
        backupsCrypt.encrypt();
    }

    private void wipe(File file) throws IOException {
        FileWiper fileWiper = new FileWiper(file);
        boolean success = fileWiper.wipeFile();

        if (!success) {
            throw new RuntimeException("Filed to wipe file");
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

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
