package app.notesr.service.data.importer.v1;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.notesr.db.dao.DataBlockDao;
import app.notesr.db.dao.FileInfoDao;
import app.notesr.service.data.importer.BaseFilesImporter;
import app.notesr.model.DataBlock;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

class FilesImporter extends BaseFilesImporter {

    public FilesImporter(JsonParser parser,
                         FileInfoDao fileInfoDao,
                         DataBlockDao dataBlockDao,
                         Map<String, String> adaptedNotesIdMap,
                         DateTimeFormatter timestampFormatter) {

        super(parser, fileInfoDao, dataBlockDao, adaptedNotesIdMap, timestampFormatter);
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    DataBlock dataBlock = parseDataBlockObject();

                    dataBlock.setData(dataBlock.getData());
                    dataBlockDao.insert(dataBlock);
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

        return dataBlock;
    }
}
