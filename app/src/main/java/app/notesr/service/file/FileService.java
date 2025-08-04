package app.notesr.service.file;

import static java.util.Objects.requireNonNull;

import app.notesr.db.AppDatabase;
import app.notesr.model.DataBlock;
import app.notesr.model.FileInfo;
import app.notesr.util.HashHelper;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FileService {
    private static final int CHUNK_SIZE = 500000;

    private final AppDatabase db;

    public long getFilesCount(String noteId) {
        Long count = db.getFileInfoDao().getCountByNoteId(noteId);

        if (count == null) {
            throw new NullPointerException("Files count is null");
        }

        return count;
    }

    public List<FileInfo> getFilesInfo(String noteId) {
        List<FileInfo> filesInfos = db.getFileInfoDao().getByNoteId(noteId);

        return filesInfos.stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public FileInfo getFileInfo(String fileId) {
        return setDecimalId(db.getFileInfoDao().get(fileId));
    }

    public void save(FileInfo fileInfo, File dataSourceFile) throws IOException {
        db.runInTransaction(() -> {
            String fileId = saveInfo(fileInfo);
            saveData(fileId, dataSourceFile);
            return null;
        });
    }

    public String saveInfo(FileInfo fileInfo) {
        return db.runInTransaction(() -> {
            fileInfo.setId(UUID.randomUUID().toString());

            db.getFileInfoDao().insert(fileInfo);
            db.getNoteDao().setUpdatedAtById(fileInfo.getNoteId(), LocalDateTime.now());

            return fileInfo.getId();
        });
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

                DataBlock dataBlock = new DataBlock();
                dataBlock.setId(UUID.randomUUID().toString());
                dataBlock.setFileId(fileId);
                dataBlock.setOrder(order);
                dataBlock.setData(chunk);

                db.getDataBlockDao().insert(dataBlock);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = stream.read(chunk);

                order++;
            }
        }
    }

    public byte[] read(String fileId) {
        Set<String> ids = new LinkedHashSet<>(db.getDataBlockDao().getBlockIdsByFileId(fileId));

        Long fileSize = requireNonNull(db.getFileInfoDao().get(fileId)).getSize();
        byte[] fileBytes = new byte[Math.toIntExact(fileSize)];
        int readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = db.getDataBlockDao().get(id);
            byte[] data = dataBlock.getData();

            System.arraycopy(data, 0, fileBytes, readBytes, data.length);
            readBytes += data.length;
        }

        return fileBytes;
    }

    public long read(String fileId, Consumer<byte[]> actionPerChunk) {
        Set<String> ids = new LinkedHashSet<>(db.getDataBlockDao().getBlockIdsByFileId(fileId));

        long readBytes = 0;

        for (String id : ids) {
            DataBlock dataBlock = db.getDataBlockDao().get(id);
            actionPerChunk.accept(dataBlock.getData());
            readBytes += dataBlock.getData().length;
        }

        return readBytes;
    }

    public void delete(String fileId) {
        FileInfo fileInfo = db.getFileInfoDao().get(fileId);

        db.runInTransaction(() -> {
            db.getDataBlockDao().deleteByFileId(fileId);
            db.getFileInfoDao().delete(fileInfo);
            db.getNoteDao().setUpdatedAtById(fileInfo.getNoteId(), LocalDateTime.now());
        });
    }

    private FileInfo setDecimalId(FileInfo fileInfo) {
        UUID uuid = UUID.fromString(fileInfo.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        fileInfo.setDecimalId(hash);

        return fileInfo;
    }
}
