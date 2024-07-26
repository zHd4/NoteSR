package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class FilesImporter {
    private final JsonParser parser;
    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;
    private final DateTimeFormatter timestampFormatter;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void importFiles() throws IOException {

    }
}
