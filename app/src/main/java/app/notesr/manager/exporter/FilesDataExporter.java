package app.notesr.manager.exporter;

import java.io.File;
import java.io.IOException;

import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.model.DataBlock;
import app.notesr.utils.FilesUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FilesDataExporter {
    private final File outputDir;
    private final DataBlocksTable dataBlocksTable;

    public void export() throws IOException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        for (DataBlock blockWithoutData : dataBlocksTable.getAllWithoutData()) {
            DataBlock dataBlock = dataBlocksTable.get(blockWithoutData.getId());

            byte[] data = FilesCrypt.decryptData(dataBlock.getData());
            FilesUtils.writeFileBytes(new File(outputDir, dataBlock.getId().toString()), data);
        }
    }
}
