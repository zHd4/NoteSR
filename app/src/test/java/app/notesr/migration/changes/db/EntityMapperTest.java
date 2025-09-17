package app.notesr.migration.changes.db;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.security.crypto.ValueDecryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityMapperTest {

    @Mock
    private ValueDecryptor valueDecryptor;

    private EntityMapper entityMapper;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        entityMapper = new EntityMapper(valueDecryptor);
        testDateTime = LocalDateTime.now();
    }

    @Test
    void testMapNoteShouldReturnValidNote() throws DecryptionFailedException {
        byte[] encryptedName = "encryptedName".getBytes();
        byte[] encryptedText = "encryptedText".getBytes();
        byte[] decryptedName = "Test Note".getBytes();
        byte[] decryptedText = "Test Content".getBytes();

        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("id", "123");
        noteMap.put("encryptedName", encryptedName);
        noteMap.put("encryptedText", encryptedText);
        noteMap.put("updatedAt", testDateTime);

        when(valueDecryptor.decrypt(encryptedName)).thenReturn(decryptedName);
        when(valueDecryptor.decrypt(encryptedText)).thenReturn(decryptedText);

        Note result = entityMapper.mapNote(noteMap);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Test Note", result.getName());
        assertEquals("Test Content", result.getText());
        assertEquals(testDateTime, result.getUpdatedAt());
    }

    @Test
    void testMapFileInfoShouldReturnValidFileInfo() throws DecryptionFailedException {
        byte[] encryptedName = "encryptedName".getBytes();
        byte[] encryptedType = "encryptedType".getBytes();
        byte[] encryptedThumbnail = "encryptedThumbnail".getBytes();
        byte[] decryptedName = "test.txt".getBytes();
        byte[] decryptedType = "text/plain".getBytes();
        byte[] decryptedThumbnail = "thumbnail".getBytes();

        Map<String, Object> fileInfoMap = new HashMap<>();
        fileInfoMap.put("id", "123");
        fileInfoMap.put("noteId", "456");
        fileInfoMap.put("encryptedName", encryptedName);
        fileInfoMap.put("encryptedType", encryptedType);
        fileInfoMap.put("encryptedThumbnail", encryptedThumbnail);
        fileInfoMap.put("size", 1000L);
        fileInfoMap.put("createdAt", testDateTime);
        fileInfoMap.put("updatedAt", testDateTime);

        when(valueDecryptor.decrypt(encryptedName)).thenReturn(decryptedName);
        when(valueDecryptor.decrypt(encryptedType)).thenReturn(decryptedType);
        when(valueDecryptor.decrypt(encryptedThumbnail)).thenReturn(decryptedThumbnail);

        FileInfo result = entityMapper.mapFileInfo(fileInfoMap);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("456", result.getNoteId());
        assertEquals("test.txt", result.getName());
        assertEquals("text/plain", result.getType());
        assertArrayEquals(decryptedThumbnail, result.getThumbnail());
        assertEquals(1000L, result.getSize());
        assertEquals(testDateTime, result.getCreatedAt());
        assertEquals(testDateTime, result.getUpdatedAt());
    }

    @Test
    void testMapDataBlockShouldReturnValidFileBlobInfo() throws DecryptionFailedException {
        byte[] encryptedData = "encryptedData".getBytes();
        byte[] decryptedData = "decryptedData".getBytes();

        Map<String, Object> dataBlockMap = new HashMap<>();
        dataBlockMap.put("id", "123");
        dataBlockMap.put("fileId", "456");
        dataBlockMap.put("order", 1L);
        dataBlockMap.put("encryptedData", encryptedData);

        FileBlobInfo result = entityMapper.mapFileBlobInfo(dataBlockMap);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("456", result.getFileId());
        assertEquals(1L, result.getOrder());
    }

    @Test
    void testGetDataOfDataBlockShouldReturnDecryptedData() throws DecryptionFailedException {
        byte[] encryptedData = "encryptedData".getBytes();
        byte[] decryptedData = "decryptedData".getBytes();

        Map<String, Object> dataBlockMap = new HashMap<>();
        dataBlockMap.put("encryptedData", encryptedData);

        when(valueDecryptor.decrypt(encryptedData)).thenReturn(decryptedData);

        byte[] result = entityMapper.getDataOfDataBlock(dataBlockMap);

        assertNotNull(result);
        assertArrayEquals(decryptedData, result);
    }

    @Test
    void testMapNoteShouldThrowExceptionWhenNoteMapIsNull() {
        assertThrows(NullPointerException.class, () -> entityMapper.mapNote(null));
    }

    @Test
    void testMapFileInfoShouldThrowExceptionWhenFileInfoMapIsNull() {
        assertThrows(NullPointerException.class, () -> entityMapper.mapFileInfo(null));
    }

    @Test
    void testMapDataBlockShouldThrowExceptionWhenDataBlockMapIsNull() {
        assertThrows(NullPointerException.class, () -> entityMapper.mapFileBlobInfo(null));
    }
}
