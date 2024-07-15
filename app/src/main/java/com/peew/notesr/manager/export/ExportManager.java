package com.peew.notesr.manager.export;

import android.content.Context;
import android.content.pm.PackageManager;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.App;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.manager.BaseManager;
import com.peew.notesr.tools.VersionFetcher;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ExportManager extends BaseManager {

    private final Context context;

    public ExportManager(Context context) {
        this.context = context;
    }

    public void export(String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputFile, JsonEncoding.UTF8);

        jsonGenerator.useDefaultPrettyPrinter();

        try (jsonGenerator) {
            jsonGenerator.writeStartObject();

            writeVersion(jsonGenerator);
            getNotesExporter(jsonGenerator, getTimestampFormatter()).writeNotes();

            jsonGenerator.writeEndObject();
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

    private NotesWriter getNotesExporter(JsonGenerator jsonGenerator, DateTimeFormatter dateTimeFormatter) {
        NotesTable notesTable = App.getAppContainer()
                .getNotesDB()
                .getTable(NotesTable.class);

        return new NotesWriter(jsonGenerator, notesTable, dateTimeFormatter);
    }

    private DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
