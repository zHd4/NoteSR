package app.notesr.manager.data.importer.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.manager.data.importer.BaseFilesImporter;
import app.notesr.model.DataBlock;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class FilesImporter extends BaseFilesImporter {

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         DateTimeFormatter timestampFormatter) {
        super(parser, filesInfoTable, dataBlocksTable, timestampFormatter);
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    DataBlock dataBlock = new DataBlock();
                    parseDataBlockObject(dataBlock);

                    if (dataBlock.getId() != null) {
                        dataBlock.setData(FilesCrypt.encryptData(dataBlock.getData()));
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

                    case "data" -> {
                        if (!parser.getValueAsString().equals("data")) {
                            byte[] data = parser.getBinaryValue();
                            dataBlock.setData(data);
                        }
                    }

                    default -> {}
                }
            }
        }
    }
}
