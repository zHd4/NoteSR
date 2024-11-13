package app.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

interface JsonWriter {
    void write() throws IOException;
    JsonGenerator getJsonGenerator();
    long getTotal();
    long getExported();
}
