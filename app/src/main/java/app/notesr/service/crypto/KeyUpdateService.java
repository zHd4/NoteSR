package app.notesr.service.crypto;

import app.notesr.crypto.CryptoManager;
import app.notesr.crypto.FileCryptor;
import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.dto.CryptoKey;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.exception.ReEncryptionFailedException;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import lombok.Getter;

import java.util.Set;

public class KeyUpdateService {

    private final CryptoManager cryptoManager;
    private final CryptoKey newKey;
    private final CryptoKey oldKey;
    private final NotesDb db;
    private final NoteDao noteDao;
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;

    @Getter
    private final long total;

    @Getter
    private long progress;

    public KeyUpdateService(CryptoManager cryptoManager, CryptoKey oldKey, CryptoKey newKey, NotesDb db) {
        this.cryptoManager = cryptoManager;
        this.newKey = newKey;
        this.oldKey = oldKey;
        this.db = db;
        this.noteDao = db.getDao(NoteDao.class);
        this.fileInfoDao = db.getDao(FileInfoDao.class);
        this.dataBlockDao = db.getDao(DataBlockDao.class);
        this.total = calculateTotal() + 1;
    }

    public void updateEncryptedData() {
        db.beginTransaction();

        try {
            noteDao.getAll().forEach(note -> {
                noteDao.save(NoteCryptor.updateKey(note, oldKey, newKey));
                progress += 1;

                fileInfoDao.getByNoteId(note.getId())
                        .forEach(fileInfo -> {
                            EncryptedFileInfo updatedFileInfo =
                                    FileCryptor.updateKey(fileInfo, oldKey, newKey);

                            Set<String> blockIds =
                                    dataBlockDao.getBlocksIdsByFileId(updatedFileInfo.getId());

                            for (String blockId : blockIds) {
                                DataBlock block = dataBlockDao.get(blockId);

                                block.setData(FileCryptor.updateKey(block.getData(), oldKey, newKey));
                                dataBlockDao.save(block);
                                progress += 1;
                            }

                            fileInfoDao.save(updatedFileInfo);
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
        return noteDao.getRowsCount()
                + fileInfoDao.getRowsCount()
                + dataBlockDao.getRowsCount();
    }
}
