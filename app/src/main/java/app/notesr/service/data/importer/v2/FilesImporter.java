package app.notesr.service.data.importer.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.crypto.FileCryptor;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
import app.notesr.service.data.importer.BaseFilesImporter;
import app.notesr.model.DataBlock;
import app.notesr.util.FilesUtils;

class FilesImporter extends BaseFilesImporter {

    private final File dataBlocksDir;

    public FilesImporter(JsonParser parser,
                         FileInfoTable fileInfoTable,
                         DataBlockTable dataBlockTable,
                         Map<String, String> adaptedNotesIdMap,
                         File dataBlocksDir,
                         DateTimeFormatter timestampFormatter) {
        super(parser, fileInfoTable, dataBlockTable, adaptedNotesIdMap, timestampFormatter);
        this.dataBlocksDir = dataBlocksDir;
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    DataBlock dataBlock = new DataBlock();
                    parseDataBlockObject(dataBlock);

                    String id = dataBlock.getId();

                    if (id != null) {
                        String dataFileName = dataBlocksIdMap.getOrDefault(id, id);
                        byte[] data = FileCryptor.encryptData(readDataBlock(dataFileName));

                        dataBlock.setData(data);
                        dataBlockTable.save(dataBlock, false);
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
                        adaptId(dataBlock);
                    }

                    case "file_id" -> {
                        if (parser.getValueAsString().equals("file_id")) continue;

                        String id = parser.getValueAsString();
                        dataBlock.setFileId(adaptedFilesIdMap.getOrDefault(id, id));
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
