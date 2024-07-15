package com.peew.notesr.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.App;
import com.peew.notesr.model.Note;
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

        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            writeVersion(jsonGenerator);
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

    private void writeNotes(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeArrayFieldStart("notes");
        jsonGenerator.writeEndArray();
    }

    private void writeNote(JsonGenerator jsonGenerator, Note note) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeFieldId(note.getId());

        jsonGenerator.writeStringField("name", note.getName());
        jsonGenerator.writeStringField("text", note.getText());

        String updatedAt = note.getUpdatedAt().format(getTimestampFormatter());
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}
