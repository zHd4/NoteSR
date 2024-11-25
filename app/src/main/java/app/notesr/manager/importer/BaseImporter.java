package app.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseImporter {
    protected final JsonParser parser;
    protected final DateTimeFormatter timestampFormatter;

    protected boolean skipTo(String targetField) throws IOException {
        String currentField = parser.getCurrentName();

        while (currentField == null || !currentField.equals(targetField)) {
            if (parser.nextToken() == null) {
                return false;
            }

            currentField = parser.getCurrentName();
        }

        return true;
    }
}
