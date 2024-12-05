package app.notesr.manager.data.importer.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.manager.data.importer.BaseFilesImporter;
import app.notesr.model.DataBlock;
import app.notesr.utils.FilesUtils;

class FilesImporter extends BaseFilesImporter {

    private final File dataBlocksDir;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         File dataBlocksDir,
                         DateTimeFormatter timestampFormatter) {
        super(parser, filesInfoTable, dataBlocksTable, timestampFormatter);
        this.dataBlocksDir = dataBlocksDir;
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    DataBlock dataBlock = new DataBlock();
                    parseDataBlockObject(dataBlock);

                    if (dataBlock.getId() != null) {
                        byte[] data = FilesCrypt.encryptData(readDataBlock(dataBlock.getId()));

                        dataBlock.setData(data);
                        dataBlocksTable.save(dataBlock);
                    }
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    @Override
    protected void parseDataBlockObject(DataBlock dataBlock) throws IOException {
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;
                        dataBlock.setId(parser.getValueAsString());
                    }

                    case "file_id" -> {
                        if (parser.getValueAsString().equals("file_id")) continue;
                        dataBlock.setFileId(parser.getValueAsString());
                    }

                    case "order" -> {
                        if (parser.getValueAsString().equals("order")) continue;
                        dataBlock.setOrder(parser.getValueAsLong());
                    }

                    default -> {}
                }
            }
        }
    }

    private byte[] readDataBlock(String id) throws IOException {
        return FilesUtils.readFileBytes(new File(dataBlocksDir, id));
    }
}
