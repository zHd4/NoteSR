package app.notesr.manager;

import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.FileInfo;
import app.notesr.utils.HashHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AssignmentsManager extends BaseManager {
    private static final int CHUNK_SIZE = 500000;

    public long getFilesCount(String noteId) {
        Long count = getFilesInfoTable().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo(String noteId) {
        List<EncryptedFileInfo> encryptedFilesInfo = getFilesInfoTable().getByNoteId(noteId);

        return FilesCrypt.decryptInfo(encryptedFilesInfo).stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public FileInfo getInfo(String fileId) {
        EncryptedFileInfo encryptedFileInfo = getFilesInfoTable().get(fileId);
        return setDecimalId(FilesCrypt.decryptInfo(encryptedFileInfo));
    }

    public String saveInfo(FileInfo fileInfo) {
        EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo);

        getFilesInfoTable().save(encryptedFileInfo);
        getNotesTable().markAsModified(encryptedFileInfo.getNoteId());

        return encryptedFileInfo.getId();
    }

    public void saveData(String fileId, InputStream stream) throws IOException {
        try (stream) {
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

                DataBlock dataBlock = DataBlock.builder()
                        .fileId(fileId)
                        .order(order)
                        .data(chunk)
                        .build();

                dataBlocksTable.save(dataBlock);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = stream.read(chunk);

                order++;
            }
        }
    }

    public byte[] read(String fileId) {
        FilesInfoTable filesInfoTable = getFilesInfoTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        Set<String> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(filesInfoTable.get(fileId).getSize())];
        int readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] blockData = FilesCrypt.decryptData(dataBlock.getData());

            System.arraycopy(blockData, 0, data, readBytes, blockData.length);
            readBytes += blockData.length;
        }

        return data;
    }

    public long read(String fileId, Consumer<byte[]> actionPerChunk) {
        DataBlocksTable dataBlocksTable = getDataBlocksTable();
        Set<String> ids = dataBlocksTable.getBlocksIdsByFileId(fileId);

        long readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlocksTable.get(id);
            byte[] data = FilesCrypt.decryptData(dataBlock.getData());

            actionPerChunk.accept(data);
            readBytes += data.length;
        }

        return readBytes;
    }

    public void delete(String fileId) {
        getDataBlocksTable().deleteByFileId(fileId);
        getNotesTable().markAsModified(getFilesInfoTable().get(fileId).getNoteId());
        getFilesInfoTable().delete(fileId);
    }

    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }
}
