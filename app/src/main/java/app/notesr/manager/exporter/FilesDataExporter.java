package app.notesr.manager.exporter;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.DataBlock;
import app.notesr.utils.FilesUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FilesDataExporter {
    private final File outputDir;
    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;

    public void export() throws IOException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new RuntimeException("Failed to create temporary directory to export data blocks");
            }
        }

        for (Long fileId : filesInfoTable.getAllIds()) {
            Set<Long> blocksId = dataBlocksTable.getBlocksIdsByFileId(fileId);

            for (Long blockId : blocksId) {
                DataBlock block = dataBlocksTable.get(blockId);

                Long id = block.getId();
                byte[] data = FilesCrypt.decryptData(block.getData());

                FilesUtils.writeFileBytes(new File(outputDir, id.toString()), data);
            }
        }
    }
}
