package app.notesr.exporter.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.db.dao.DataBlockDao;
import app.notesr.file.model.DataBlock;
import app.notesr.util.FilesUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class FilesDataExporter implements Exporter {
    private final File outputDir;
    private final DataBlockDao dataBlockDao;
    private final Runnable checkCancelled;

    @Getter
    private long exported = 0;

    @Override
    public void export() throws IOException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        List<DataBlock> dataBlocksWithoutData = dataBlockDao.getAllWithoutData();

        for (DataBlock blockWithoutData : dataBlocksWithoutData) {
            DataBlock dataBlock = dataBlockDao.get(blockWithoutData.getId());
            FilesUtils.writeFileBytes(new File(outputDir, dataBlock.getId()), dataBlock.getData());

            exported++;
            checkCancelled.run();
        }
    }

    @Override
    public long getTotal() {
        return dataBlockDao.getRowsCount();
    }
}
