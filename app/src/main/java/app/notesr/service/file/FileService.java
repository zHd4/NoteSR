package app.notesr.service.file;

import static java.util.Objects.requireNonNull;

import app.notesr.App;
import app.notesr.crypto.FileCryptor;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.FileInfo;
import app.notesr.util.HashHelper;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FileService {
    private static final int CHUNK_SIZE = 500000;

    private final NoteDao noteDao;
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;

    public long getFilesCount(String noteId) {
        Long count = fileInfoDao.getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo(String noteId) {
        List<EncryptedFileInfo> encryptedFilesInfo = fileInfoDao.getByNoteId(noteId);

        return FileCryptor.decryptInfo(encryptedFilesInfo).stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public FileInfo getInfo(String fileId) {
        EncryptedFileInfo encryptedFileInfo = fileInfoDao.get(fileId);
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

        fileInfoDao.save(encryptedFileInfo);
        noteDao.markAsModified(encryptedFileInfo.getNoteId());

        return encryptedFileInfo.getId();
    }

    public void saveData(String fileId, File sourceFile) throws IOException {
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

                dataBlockDao.save(dataBlock);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = stream.read(chunk);

                order++;
            }
        }
    }

    public byte[] read(String fileId) {

        Set<String> ids = dataBlockDao.getBlocksIdsByFileId(fileId);

        byte[] data = new byte[Math.toIntExact(requireNonNull(fileInfoDao.get(fileId)).getSize())];
        int readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlockDao.get(id);
            byte[] blockData = FileCryptor.decryptData(dataBlock.getData());

            System.arraycopy(blockData, 0, data, readBytes, blockData.length);
            readBytes += blockData.length;
        }

        return data;
    }

    public long read(String fileId, Consumer<byte[]> actionPerChunk) {
        Set<String> ids = dataBlockDao.getBlocksIdsByFileId(fileId);

        long readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = dataBlockDao.get(id);
            byte[] data = FileCryptor.decryptData(dataBlock.getData());

            actionPerChunk.accept(data);
            readBytes += data.length;
        }

        return readBytes;
    }

    public void delete(String fileId) {
        NotesDb db = App.getAppContainer().getNotesDB();
        db.beginTransaction();

        dataBlockDao.deleteByFileId(fileId);
        noteDao.markAsModified(requireNonNull(fileInfoDao.get(fileId)).getNoteId());
        fileInfoDao.delete(fileId);

        db.commitTransaction();
    }

    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }
}
