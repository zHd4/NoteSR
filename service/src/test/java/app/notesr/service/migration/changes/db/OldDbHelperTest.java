package app.notesr.service.migration.changes.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OldDbHelperTest {

    @Mock
    private Context mockContext;
    @Mock
    private SQLiteDatabase mockDb;
    @Mock
    private Cursor mockCursor;

    private OldDbHelper oldDbHelper;

    @BeforeEach
    void setUp() {
        oldDbHelper = new OldDbHelper(mockContext) {
            @Override
            public SQLiteDatabase getReadableDatabase() {
                return mockDb;
            }
        };
    }

    @Test
    void testGetAllNotesWhenNotesExistReturnsNotesList() {
        when(mockDb.rawQuery(eq("SELECT * FROM notes"), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(false);
        when(mockCursor.getString(0)).thenReturn("note1");
        when(mockCursor.getBlob(1)).thenReturn(new byte[]{1, 2, 3});
        when(mockCursor.getBlob(2)).thenReturn(new byte[]{4, 5, 6});
        when(mockCursor.getString(3)).thenReturn("2023-01-01 12:00:00");

        List<Map<String, Object>> result = oldDbHelper.getAllNotes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("note1", result.get(0).get("id"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) result.get(0).get("encryptedName"));
    }

    @Test
    void testGetFilesInfoWhenFilesExistReturnsFilesList() {
        when(mockDb.rawQuery(eq("SELECT * FROM files_info"), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(false);
        when(mockCursor.getString(0)).thenReturn("file1");
        when(mockCursor.getString(1)).thenReturn("note1");
        when(mockCursor.getBlob(2)).thenReturn(new byte[]{1, 2, 3});
        when(mockCursor.getBlob(3)).thenReturn(new byte[]{4, 5, 6});
        when(mockCursor.getBlob(4)).thenReturn(new byte[]{7, 8, 9});
        when(mockCursor.getLong(5)).thenReturn(1000L);
        when(mockCursor.getString(6)).thenReturn("2023-01-01 12:00:00");
        when(mockCursor.getString(7)).thenReturn("2023-01-01 12:00:00");

        List<Map<String, Object>> result = oldDbHelper.getFilesInfo();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("file1", result.get(0).get("id"));
        assertEquals("note1", result.get(0).get("noteId"));
        assertEquals(1000L, result.get(0).get("size"));
    }

    @Test
    void testGetBlocksIdsByFileIdWhenBlocksExistReturnsBlockIds() {
        String query = "SELECT id FROM data_blocks WHERE file_id = ? ORDER BY block_order";

        when(mockDb.rawQuery(eq(query), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(true, false);
        when(mockCursor.getString(0)).thenReturn("block1", "block2");

        List<String> result = oldDbHelper.getBlocksIdsByFileId("file1");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("block1", result.get(0));
        assertEquals("block2", result.get(1));
    }

    @Test
    void testGetDataBlockByIdWhenBlockExistsReturnsBlock() {
        String query = "SELECT file_id, block_order, data FROM data_blocks WHERE id = ?";

        when(mockDb.rawQuery(eq(query), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getString(0)).thenReturn("file1");
        when(mockCursor.getLong(1)).thenReturn(1L);
        when(mockCursor.getBlob(2)).thenReturn(new byte[] {1, 2, 3});

        Map<String, Object> result = oldDbHelper.getDataBlockById("block1");

        assertNotNull(result);
        assertEquals("file1", result.get("fileId"));
        assertEquals(1L, result.get("order"));
        assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) result.get("encryptedData"));
    }

    @Test
    void testGetDataBlockByIdWhenBlockDoesNotExistReturnsNull() {
        String query = "SELECT file_id, block_order, data FROM data_blocks WHERE id = ?";

        when(mockDb.rawQuery(eq(query), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);

        Map<String, Object> result = oldDbHelper.getDataBlockById("nonexistent");

        assertNull(result);
    }
}
