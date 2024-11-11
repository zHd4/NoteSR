package app.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public interface Writer {
    void write() throws IOException;
    JsonGenerator getJsonGenerator();
    long getTotal();
    long getExported();
}
