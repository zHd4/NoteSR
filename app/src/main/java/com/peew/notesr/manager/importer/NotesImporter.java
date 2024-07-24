package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.tools.ProgressCounter;

import java.time.format.DateTimeFormatter;

class NotesImporter {
    private final JsonParser parser;
    private final ProgressCounter progressCounter;

    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;

    public NotesImporter(JsonParser parser,
                         ProgressCounter progressCounter,
                         NotesTable notesTable,
                         DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.progressCounter = progressCounter;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void importNotes() {

    }
}
