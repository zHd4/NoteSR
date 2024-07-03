package com.peew.notesr.manager;

import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesInfoTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.EncryptedNote;

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
