package app.notesr.importer.service.v2;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.file.service.FileService;
import app.notesr.importer.service.BaseFilesImporter;
import app.notesr.file.model.DataBlock;
import app.notesr.util.FilesUtils;

class FilesV2Importer extends BaseFilesImporter {

    private final File dataBlocksDir;

    public FilesV2Importer(JsonParser parser,
                           FileService fileService,
                           Map<String, String> adaptedNotesIdMap,
                           File dataBlocksDir,
                           DateTimeFormatter timestampFormatter) {
        super(parser, fileService, adaptedNotesIdMap, timestampFormatter);
        this.dataBlocksDir = dataBlocksDir;
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    DataBlock dataBlock = parseDataBlockObject();
                    String id = dataBlock.getId();

                    String dataFileName = adaptedDataBlocksIdMap.getOrDefault(id, id);
                    dataBlock.setData(readFile(dataFileName));
                    fileService.importDataBlock(dataBlock);
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    @Override
    protected DataBlock parseDataBlockObject() throws IOException {
        DataBlock dataBlock = new DataBlock();
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
                        dataBlock.setFileId(requireNonNull(adaptedFilesIdMap.getOrDefault(id, id)));
                    }

                    case "order" -> {
                        if (parser.getValueAsString().equals("order")) continue;
                        dataBlock.setOrder(parser.getValueAsLong());
                    }

                    default -> {}
                }
            }
        }

        return dataBlock;
    }

    private byte[] readFile(String id) throws IOException {
        return new FilesUtils().readFileBytes(new File(dataBlocksDir, id));
    }
}
