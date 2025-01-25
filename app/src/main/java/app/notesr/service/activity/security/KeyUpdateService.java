package app.notesr.service.activity.security;

import app.notesr.dto.CryptoKey;
import app.notesr.crypto.FilesCrypt;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.EncryptedNote;
import app.notesr.service.ServiceBase;

import java.util.Set;
import java.util.function.Consumer;

public class KeyUpdateService extends ServiceBase {
    public void updateEncryptedData(CryptoKey oldKey, CryptoKey newKey) {
        getNotesTable()
                .getAll()
                .forEach(getNotesUpdateCallback(oldKey, newKey));
    }

    private Consumer<EncryptedNote> getNotesUpdateCallback(CryptoKey oldKey, CryptoKey newKey) {
        return note -> {
            getNotesTable().save(NotesCrypt.updateKey(note, oldKey, newKey));
            getFilesInfoTable()
                    .getByNoteId(note.getId())
                    .forEach(getFilesInfoUpdateCallback(oldKey, newKey));
        };
    }

    private Consumer<EncryptedFileInfo> getFilesInfoUpdateCallback(CryptoKey oldKey, CryptoKey newKey) {
        return fileInfo -> {
            FilesInfoTable filesInfoTable = getFilesInfoTable();
            DataBlocksTable dataBlocksTable = getDataBlocksTable();

            EncryptedFileInfo updatedFileInfo = FilesCrypt.updateKey(fileInfo, oldKey, newKey);
            Set<String> blockIds = dataBlocksTable.getBlocksIdsByFileId(updatedFileInfo.getId());

            for (String blockId : blockIds) {
                DataBlock block = dataBlocksTable.get(blockId);

                block.setData(FilesCrypt.updateKey(block.getData(), oldKey, newKey));
                dataBlocksTable.save(block);
            }

            filesInfoTable.save(updatedFileInfo);
        };
    }
}
