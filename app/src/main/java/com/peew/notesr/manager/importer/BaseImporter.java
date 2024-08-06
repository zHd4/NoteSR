package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class BaseImporter {
    protected final JsonParser parser;
    protected final DateTimeFormatter timestampFormatter;

    public BaseImporter(JsonParser parser, DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.timestampFormatter = timestampFormatter;
    }

    protected void skipTo(String targetField) throws IOException {
        String currentField = parser.getCurrentName();

        while (currentField == null || !currentField.equals(targetField)) {
            parser.nextToken();
            currentField = parser.getCurrentName();
        }
    }
}
