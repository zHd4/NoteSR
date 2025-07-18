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
    private final DataBlockDao dataBlockDao;

    FilesDataExporter(ExportThread thread, File outputDir, DataBlockDao dataBlockDao) {
        super(thread);

        this.outputDir = outputDir;
        this.dataBlockDao = dataBlockDao;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        List<DataBlock> dataBlocksWithoutData = dataBlockDao.getAllWithoutData();

        for (DataBlock blockWithoutData : dataBlocksWithoutData) {
            DataBlock dataBlock = dataBlockDao.get(blockWithoutData.getId());
            byte[] data = FileCryptor.decryptData(dataBlock.getData());

            FilesUtils.writeFileBytes(new File(outputDir, dataBlock.getId()), data);

            increaseExported();
            getThread().breakOnInterrupted();
        }
    }

    @Override
    long getTotal() {
        return dataBlockDao.getRowsCount();
    }
}
