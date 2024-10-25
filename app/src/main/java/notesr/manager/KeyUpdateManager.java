package notesr.manager;

import notesr.crypto.CryptoKey;
import notesr.crypto.FilesCrypt;
import notesr.crypto.NotesCrypt;
import notesr.db.notes.table.DataBlocksTable;
import notesr.db.notes.table.FilesInfoTable;
import notesr.model.DataBlock;
import notesr.model.EncryptedFileInfo;
import notesr.model.EncryptedNote;

import java.util.Set;
import java.util.function.Consumer;

public class KeyUpdateManager extends BaseManager {
    public void updateEncryptedData(CryptoKey oldKey, CryptoKey newKey) {
        getNotesTable()
                .getAll()
                .forEach(noteUpdater(oldKey, newKey));
    }

    private Consumer<EncryptedNote> noteUpdater(CryptoKey oldKey, CryptoKey newKey) {
        return note -> {
            getNotesTable().save(NotesCrypt.updateKey(note, oldKey, newKey));
            getFilesInfoTable()
                    .getByNoteId(note.getId())
                    .forEach(fileInfoUpdater(oldKey, newKey));
        };
    }

    private Consumer<EncryptedFileInfo> fileInfoUpdater(CryptoKey oldKey, CryptoKey newKey) {
        return fileInfo -> {
            FilesInfoTable filesInfoTable = getFilesInfoTable();
            DataBlocksTable dataBlocksTable = getDataBlocksTable();

            EncryptedFileInfo updatedFileInfo = FilesCrypt.updateKey(fileInfo, oldKey, newKey);
            Set<Long> blockIds = dataBlocksTable.getBlocksIdsByFileId(updatedFileInfo.getId());

            for (Long blockId : blockIds) {
                DataBlock block = dataBlocksTable.get(blockId);

                block.setData(FilesCrypt.updateKey(block.getData(), oldKey, newKey));
                dataBlocksTable.save(block);
            }

            filesInfoTable.save(updatedFileInfo);
        };
    }
}
