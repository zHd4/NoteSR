package app.notesr.service.activity.security;

import app.notesr.App;
import app.notesr.crypto.CryptoManager;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.notes.table.NoteTable;
import app.notesr.dto.CryptoKey;
import app.notesr.crypto.FileCryptor;
import app.notesr.crypto.NoteCrypt;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
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
        this.total = calculateTotal() + 1;
    }

    public void updateEncryptedData() {
        NotesDB db = getNotesDB();

        NoteTable noteTable = getNoteTable();
        FileInfoTable fileInfoTable = getFileInfoTable();
        DataBlockTable dataBlockTable = getDataBlockTable();

        db.beginTransaction();

        try {
            noteTable.getAll().forEach(note -> {
                noteTable.save(NoteCrypt.updateKey(note, oldKey, newKey));
                progress += 1;

                fileInfoTable.getByNoteId(note.getId())
                        .forEach(fileInfo -> {
                            EncryptedFileInfo updatedFileInfo =
                                    FileCryptor.updateKey(fileInfo, oldKey, newKey);

                            Set<String> blockIds =
                                    dataBlockTable.getBlocksIdsByFileId(updatedFileInfo.getId());

                            for (String blockId : blockIds) {
                                DataBlock block = dataBlockTable.get(blockId);

                                block.setData(FileCryptor.updateKey(block.getData(), oldKey, newKey));
                                dataBlockTable.save(block);
                                progress += 1;
                            }

                            fileInfoTable.save(updatedFileInfo);
                            progress += 1;
                        });
            });

            db.commitTransaction();

            cryptoManager.applyNewKey(newKey);
            progress += 1;
        } catch (Exception e) {
            db.rollbackTransaction();
            throw new ReEncryptionFailedException(e);
        }
    }

    private long calculateTotal() {
        return getNoteTable().getRowsCount()
                + getFileInfoTable().getRowsCount()
                + getDataBlockTable().getRowsCount();
    }

    private NotesDB getNotesDB() {
        return App.getAppContainer().getNotesDB();
    }
}
