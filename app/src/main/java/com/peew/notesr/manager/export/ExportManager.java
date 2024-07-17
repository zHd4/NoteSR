package com.peew.notesr.manager.export;

import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Environment;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ExportManager extends BaseManager {

    private final Context context;

    public ExportManager(Context context) {
        this.context = context;
    }

    public void export(String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputFile, JsonEncoding.UTF8);

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

    private File getDumpFile() {
        LocalDateTime now = LocalDateTime.now();

        String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String filename = "nsr_export_" + nowStr + ".notesr.bak";

        File filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Path dumpPath = Paths.get(filesDir.toPath().toString(), filename);

        return new File(dumpPath.toUri());
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
