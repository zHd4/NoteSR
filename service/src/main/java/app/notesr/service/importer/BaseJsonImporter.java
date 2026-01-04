/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseJsonImporter {
    protected final JsonParser parser;
    protected final DateTimeFormatter timestampFormatter;

    protected final boolean skipTo(String targetField) throws IOException {
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
