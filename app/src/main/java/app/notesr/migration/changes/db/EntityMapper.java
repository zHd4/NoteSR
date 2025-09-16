package app.notesr.migration.changes.db;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.Map;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.security.crypto.ValueDecryptor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityMapper {

    private final ValueDecryptor valueDecryptor;

    public Note mapNote(Map<String, Object> noteMap) throws DecryptionFailedException {
        requireNonNull(noteMap, "Note cannot be null");

        String id = (String) noteMap.get("id");

        byte[] encryptedName = (byte[]) noteMap.get("encryptedName");
        byte[] encryptedText = (byte[]) noteMap.get("encryptedText");

        LocalDateTime updatedAt = (LocalDateTime) noteMap.get("updatedAt");

        byte[] decryptedName = valueDecryptor.decrypt(encryptedName);
        byte[] decryptedText = valueDecryptor.decrypt(encryptedText);

        Note note = new Note();

        requireNonNull(id, "Note id cannot be null");
        note.setId(id);

        note.setName(new String(decryptedName));
        note.setText(new String(decryptedText));
        note.setCreatedAt(updatedAt);
        note.setUpdatedAt(updatedAt);

        return note;
    }

    public FileInfo mapFileInfo(Map<String, Object> fileInfoMap) throws DecryptionFailedException {
        requireNonNull(fileInfoMap, "File info cannot be null");

        String id = (String) fileInfoMap.get("id");
        String noteId = (String) fileInfoMap.get("noteId");

        byte[] encryptedName = (byte[]) fileInfoMap.get("encryptedName");
        byte[] encryptedType = (byte[]) fileInfoMap.get("encryptedType");
        byte[] encryptedThumbnail = (byte[]) fileInfoMap.get("encryptedThumbnail");

        Long size = (Long) fileInfoMap.get("size");

        LocalDateTime createdAt = (LocalDateTime) fileInfoMap.get("createdAt");
        LocalDateTime updatedAt = (LocalDateTime) fileInfoMap.get("updatedAt");

        FileInfo fileInfo = new FileInfo();

        requireNonNull(id, "File id cannot be null");
        fileInfo.setId(id);

        requireNonNull(noteId, "Note id cannot be null");
        fileInfo.setNoteId(noteId);

        requireNonNull(encryptedName, "File name cannot be null");
        fileInfo.setName(new String(valueDecryptor.decrypt(encryptedName)));

        String type = null;
        byte[] thumbnail = null;

        if (encryptedType != null) {
            type = new String(valueDecryptor.decrypt(encryptedType));
        }

        if (encryptedThumbnail != null) {
            thumbnail = valueDecryptor.decrypt(encryptedThumbnail);
        }

        fileInfo.setType(type);
        fileInfo.setThumbnail(thumbnail);

        requireNonNull(size, "File size cannot be null");
        fileInfo.setSize(size);

        requireNonNull(createdAt, "File creation timestamp cannot be null");
        fileInfo.setCreatedAt(createdAt);

        requireNonNull(updatedAt, "File update timestamp cannot be null");
        fileInfo.setUpdatedAt(updatedAt);

        return fileInfo;
    }

    public FileBlobInfo mapFileBlobInfo(Map<String, Object> dataBlockMap)
            throws DecryptionFailedException {

        requireNonNull(dataBlockMap, "Data block cannot be null");

        String id = (String) dataBlockMap.get("id");
        String fileId = (String) dataBlockMap.get("fileId");
        Long order = (Long) dataBlockMap.get("order");

        FileBlobInfo dataBlock = new FileBlobInfo();

        requireNonNull(id, "Data block id cannot be null");
        dataBlock.setId(id);

        requireNonNull(fileId, "File id cannot be null");
        dataBlock.setFileId(fileId);

        requireNonNull(order, "Block order cannot be null");
        dataBlock.setOrder(order);

        return dataBlock;
    }

    public byte[] getDataOfDataBlock(Map<String, Object> dataBlockMap)
            throws DecryptionFailedException {

        requireNonNull(dataBlockMap, "Data block cannot be null");
        byte[] encryptedData = (byte[]) dataBlockMap.get("encryptedData");

        return valueDecryptor.decrypt(encryptedData);
    }
}
