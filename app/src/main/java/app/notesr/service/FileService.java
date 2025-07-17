package app.notesr.service;

import app.notesr.App;
import app.notesr.crypto.FileCryptor;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.table.DataBlockDao;
import app.notesr.db.notes.table.FileInfoDao;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.dto.FileInfo;
import app.notesr.util.HashHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FileService extends ServiceBase {
    private static final int CHUNK_SIZE = 500000;

    public long getFilesCount(String noteId) {
        Long count = getFileInfoTable().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo(String noteId) {
        List<EncryptedFileInfo> encryptedFilesInfo = getFileInfoTable().getByNoteId(noteId);

        return FileCryptor.decryptInfo(encryptedFilesInfo).stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public FileInfo getInfo(String fileId) {
        EncryptedFileInfo encryptedFileInfo = getFileInfoTable().get(fileId);
        return setDecimalId(FileCryptor.decryptInfo(encryptedFileInfo));
    }

    public void save(FileInfo fileInfo, File dataSourceFile) throws IOException {
        NotesDb db = App.getAppContainer().getNotesDB();

        db.beginTransaction();
        String fileId = saveInfo(fileInfo);

        try {
            saveData(fileId, dataSourceFile);
        } catch (IOException e) {
            db.rollbackTransaction();
            throw e;
        }

        db.commitTransaction();
    }

    public String saveInfo(FileInfo fileInfo) {
        EncryptedFileInfo encryptedFileInfo = FileCryptor.encryptInfo(fileInfo);

        getFileInfoTable().save(encryptedFileInfo);
        getNoteTable().markAsModified(encryptedFileInfo.getNoteId());

        return encryptedFileInfo.getId();
    }

    public void saveData(String fileId, File sourceFile) throws IOException {
        DataBlockDao dataBlockTable = getDataBlockTable();

        try (FileInputStream stream = new FileInputStream(sourceFile)) {
            byte[] chunk = new byte[CHUNK_SIZE];

            long order = 0;
            int bytesRead = stream.read(chunk);

            while (bytesRead != -1) {
                if (bytesRead != CHUNK_SIZE) {
                    byte[] subChunk = new byte[bytesRead];
                    System.arraycopy(chunk, 0, subChunk, 0, bytesRead);
                    chunk = subChunk;
                }

                chunk = FileCryptor.encryptData(chunk);

                DataBlock dataBlock = DataBlock.builder()
                        .fileId(fileId)
                        .order(order)
                        .data(chunk)
                        .build();

                dataBlockTable.save(dataBlock);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = stream.read(chunk);

                order++;
            }
        }
    }

    public byte[] read(String fileId) {
        FileInfoDao fileInfoTable = getFileInfoTable();
        DataBlockDao dataBlockTable = getDataBlockTable();

        Set<String> ids = dataBlockTable.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(fileInfoTable.get(fileId).getSize())];
        int readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlockTable.get(id);
            byte[] blockData = FileCryptor.decryptData(dataBlock.getData());

            System.arraycopy(blockData, 0, data, readBytes, blockData.length);
            readBytes += blockData.length;
        }

        return data;
    }

    public long read(String fileId, Consumer<byte[]> actionPerChunk) {
        DataBlockDao dataBlockTable = getDataBlockTable();
        Set<String> ids = dataBlockTable.getBlocksIdsByFileId(fileId);

        long readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlockTable.get(id);
            byte[] data = FileCryptor.decryptData(dataBlock.getData());

            actionPerChunk.accept(data);
            readBytes += data.length;
        }

        return readBytes;
    }

    public void delete(String fileId) {
        NotesDb db = App.getAppContainer().getNotesDB();
        db.beginTransaction();

        getDataBlockTable().deleteByFileId(fileId);
        getNoteTable().markAsModified(getFileInfoTable().get(fileId).getNoteId());
        getFileInfoTable().delete(fileId);

        db.commitTransaction();
    }

    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }
}
