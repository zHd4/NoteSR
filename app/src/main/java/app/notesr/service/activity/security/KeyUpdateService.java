package app.notesr.service.activity.security;

import app.notesr.App;
import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.dto.CryptoKey;
import app.notesr.crypto.FilesCrypt;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.exception.ReEncryptionFailedException;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.service.ServiceBase;
import lombok.Getter;

import java.util.Set;

public class KeyUpdateService extends ServiceBase {

    private final CryptoManager cryptoManager;
    private final CryptoKey newKey;
    private final CryptoKey oldKey;

    @Getter
    private final long total;

    @Getter
    private long progress;

    public KeyUpdateService(CryptoKey newKey) throws CloneNotSupportedException {
        this.cryptoManager = App.getAppContainer().getCryptoManager();
        this.newKey = newKey;
        this.oldKey = cryptoManager.getCryptoKeyInstance().clone();
        this.total = calculateTotal();
    }

    public void updateEncryptedData() {
        NotesDB db = getNotesDB();

        NotesTable notesTable = getNotesTable();
        FilesInfoTable filesInfoTable = getFilesInfoTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        db.beginTransaction();

        try {
            cryptoManager.applyNewKey(newKey);

            notesTable.getAll().forEach(note -> {
                notesTable.save(NotesCrypt.updateKey(note, oldKey, newKey));
                increaseProgress();

                filesInfoTable.getByNoteId(note.getId())
                        .forEach(fileInfo -> {
                            EncryptedFileInfo updatedFileInfo =
                                    FilesCrypt.updateKey(fileInfo, oldKey, newKey);

                            Set<String> blockIds =
                                    dataBlocksTable.getBlocksIdsByFileId(updatedFileInfo.getId());

                            for (String blockId : blockIds) {
                                DataBlock block = dataBlocksTable.get(blockId);

                                block.setData(FilesCrypt.updateKey(block.getData(), oldKey, newKey));
                                dataBlocksTable.save(block);
                                increaseProgress();
                            }

                            filesInfoTable.save(updatedFileInfo);
                            increaseProgress();
                        });
            });

            db.commitTransaction();
        } catch (Exception e) {
            db.rollbackTransaction();
            throw new ReEncryptionFailedException(e);
        }
    }

    private void increaseProgress() {
        progress += 1;
    }

    private long calculateTotal() {
        return getNotesTable().getRowsCount()
                + getFilesInfoTable().getRowsCount()
                + getDataBlocksTable().getRowsCount();
    }

    private NotesDB getNotesDB() {
        return App.getAppContainer().getNotesDB();
    }
}
