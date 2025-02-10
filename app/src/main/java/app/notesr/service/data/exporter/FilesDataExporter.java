package app.notesr.service.data.exporter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.crypto.FileCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.model.DataBlock;
import app.notesr.utils.FilesUtils;

class FilesDataExporter extends Exporter {
    private final File outputDir;
    private final DataBlocksTable dataBlocksTable;

    FilesDataExporter(ExportThread thread, File outputDir, DataBlocksTable dataBlocksTable) {
        super(thread);

        this.outputDir = outputDir;
        this.dataBlocksTable = dataBlocksTable;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        List<DataBlock> dataBlocksWithoutData = dataBlocksTable.getAllWithoutData();

        for (DataBlock blockWithoutData : dataBlocksWithoutData) {
            DataBlock dataBlock = dataBlocksTable.get(blockWithoutData.getId());
            byte[] data = FileCrypt.decryptData(dataBlock.getData());

            FilesUtils.writeFileBytes(new File(outputDir, dataBlock.getId().toString()), data);

            increaseExported();
            getThread().breakOnInterrupted();
        }
    }

    @Override
    long getTotal() {
        return dataBlocksTable.getRowsCount();
    }
}
