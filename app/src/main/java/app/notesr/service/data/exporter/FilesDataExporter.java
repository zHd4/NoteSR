package app.notesr.service.data.exporter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.crypto.FileCryptor;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.model.DataBlock;
import app.notesr.util.FilesUtils;

class FilesDataExporter extends BaseExporter {
    private final File outputDir;
    private final DataBlockDao dataBlockTable;

    FilesDataExporter(ExportThread thread, File outputDir, DataBlockDao dataBlockTable) {
        super(thread);

        this.outputDir = outputDir;
        this.dataBlockTable = dataBlockTable;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        List<DataBlock> dataBlocksWithoutData = dataBlockTable.getAllWithoutData();

        for (DataBlock blockWithoutData : dataBlocksWithoutData) {
            DataBlock dataBlock = dataBlockTable.get(blockWithoutData.getId());
            byte[] data = FileCryptor.decryptData(dataBlock.getData());

            FilesUtils.writeFileBytes(new File(outputDir, dataBlock.getId()), data);

            increaseExported();
            getThread().breakOnInterrupted();
        }
    }

    @Override
    long getTotal() {
        return dataBlockTable.getRowsCount();
    }
}
