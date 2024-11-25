package app.notesr.manager.importer.v2;

import com.fasterxml.jackson.core.JsonParser;

import java.time.format.DateTimeFormatter;

import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.manager.importer.BaseImporter;

class FilesImporter extends BaseImporter {

    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);

        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
    }
}
