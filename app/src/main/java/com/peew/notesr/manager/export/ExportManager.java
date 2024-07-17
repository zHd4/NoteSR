package com.peew.notesr.manager.export;

import android.content.Context;
import android.content.pm.PackageManager;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.App;
import com.peew.notesr.crypto.BackupsCrypt;
import com.peew.notesr.manager.BaseManager;
import com.peew.notesr.tools.FileWiper;
import com.peew.notesr.tools.VersionFetcher;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ExportManager extends BaseManager {

    private final Context context;

    public ExportManager(Context context) {
        this.context = context;
    }

    public void export(File outputFile) throws IOException {
        File jsonTempFile = generateTempJson();

        encrypt(jsonTempFile, outputFile);
        wipe(jsonTempFile);
    }

    private File generateTempJson() throws IOException {
        File file = File.createTempFile("export", ".json");

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(file, JsonEncoding.UTF8);

        NotesWriter notesWriter = new NotesWriter(
                jsonGenerator,
                getNotesTable(),
                getTimestampFormatter()
        );

        FilesWriter filesWriter = new FilesWriter(
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
