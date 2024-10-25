package notesr.manager;

import notesr.crypto.FilesCrypt;
import notesr.db.notes.table.DataBlocksTable;
import notesr.db.notes.table.FilesInfoTable;
import notesr.model.DataBlock;
import notesr.model.EncryptedFileInfo;
import notesr.model.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class AssignmentsManager extends BaseManager {
    private static final int CHUNK_SIZE = 500000;

    public long getFilesCount(long noteId) {
        Long count = getFilesInfoTable().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo(Long noteId) {
        List<EncryptedFileInfo> encryptedFilesInfo = getFilesInfoTable()
                .getByNoteId(noteId);

        return FilesCrypt.decryptInfo(encryptedFilesInfo);
    }

    public FileInfo getInfo(Long fileId) {
        EncryptedFileInfo encryptedFileInfo = getFilesInfoTable()
                .get(fileId);

        return FilesCrypt.decryptInfo(encryptedFileInfo);
    }

    public Long saveInfo(FileInfo fileInfo) {
        EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo);

        getFilesInfoTable().save(encryptedFileInfo);
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
        FilesInfoTable filesInfoTable = getFilesInfoTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        Set<Long> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(filesInfoTable.get(fileId).getSize())];
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
        getFilesInfoTable().delete(fileId);
    }
}
