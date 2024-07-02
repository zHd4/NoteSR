package com.peew.notesr.component;

import com.peew.notesr.App;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;

public class AssignmentsManager {
    private static final int CHUNK_SIZE = 500000;

    public Long saveInfo(FileInfo fileInfo) {
        EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo);

        getFilesTable().save(encryptedFileInfo);
        return encryptedFileInfo.getId();
    }

    public void saveData(Long fileId, InputStream stream) throws IOException {
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        byte[] chunk = new byte[CHUNK_SIZE];

        long order = 0;
        int bytesRead = stream.read(chunk);

        while (bytesRead != -1) {
            if (bytesRead != CHUNK_SIZE) {
                byte[] subChunk = new byte[bytesRead];
                System.arraycopy(chunk, 0, subChunk, 0, bytesRead);
                chunk = subChunk;
            }

            chunk = FilesCrypt.encryptData(chunk);
            dataBlocksTable.save(new DataBlock(fileId, order, chunk));

            chunk = new byte[CHUNK_SIZE];
            bytesRead = stream.read(chunk);

            order++;
        }

        stream.close();
    }

    public byte[] read(Long fileId) {
        FilesTable filesTable = getFilesTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        Set<Long> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(filesTable.get(fileId).getSize())];
        int readBytes = 0;

        for (Long id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] blockData = FilesCrypt.decryptData(dataBlock.getData());

            System.arraycopy(blockData, 0, data, readBytes, blockData.length);
            readBytes += blockData.length;
        }

        return data;
    }

    public long read(Long fileId, Consumer<byte[]> actionPerChunk) {
        DataBlocksTable dataBlocksTable = getDataBlocksTable();
        Set<Long> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        long readBytes = 0;

        for (Long id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] data = FilesCrypt.decryptData(dataBlock.getData());

            actionPerChunk.accept(data);
            readBytes += data.length;
        }

        return readBytes;
    }

    public void delete(Long fileId) {
        getDataBlocksTable().deleteByFileId(fileId);
    }

    private FilesTable getFilesTable() {
        return App.getAppContainer().getNotesDatabase().getTable(FilesTable.class);
    }

    private DataBlocksTable getDataBlocksTable() {
        return App.getAppContainer().getNotesDatabase().getTable(DataBlocksTable.class);
    }
}
