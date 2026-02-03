/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration.changes.db;

import android.content.Context;
import app.notesr.data.AppDatabase;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;
import app.notesr.service.migration.AppMigrationException;
import app.notesr.data.model.Note;
import app.notesr.service.note.NoteService;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.WiperAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomIntegrationMigrationTest {

    @Mock private Context mockContext;
    @Mock private AppDatabase mockDb;
    @Mock private NoteService mockNoteService;
    @Mock private FileService mockFileService;
    @Mock private OldDbHelper mockOldDbHelper;
    @Mock private EntityMapper mockEntityMapper;
    @Mock private FilesUtilsAdapter mockFilesUtils;
    @Mock private WiperAdapter mockWiper;
    @Mock private CryptoSecrets mockCryptoSecrets;

    private RoomIntegrationMigration migration;

    @BeforeEach
    void setup() {
        migration = spy(new RoomIntegrationMigration(1, 2));

        doReturn(mockDb).when(migration).getAppDatabase(any());
        doReturn(mockNoteService).when(migration).getNoteService(any());
        doReturn(mockFileService).when(migration).getFileService(any(), any(), any());
        doReturn(mockOldDbHelper).when(migration).getOldDbHelper(any());
        doReturn(mockEntityMapper).when(migration).getMapper(any());
        doReturn(mockFilesUtils).when(migration).getFilesUtils();
        doReturn(mockWiper).when(migration).getWiper();
        doReturn(mockCryptoSecrets).when(migration).getCryptoSecrets(mockContext);
    }

    @Test
    void testMigrateSuccessful() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        when(mockOldDbHelper.getAllNotes()).thenReturn(List.of());
        when(mockOldDbHelper.getFilesInfo()).thenReturn(List.of());

        assertDoesNotThrow(() -> migration.migrate(mockContext));

        verify(mockDb).runInTransaction(any(Runnable.class));
    }

    @Test
    void testMigrateNotesSuccessful() throws DecryptionFailedException {
        Map<String, Object> noteMap = Map.of("id", "1");
        Note note = new Note();
        
        when(mockOldDbHelper.getAllNotes()).thenReturn(List.of(noteMap));
        when(mockEntityMapper.mapNote(noteMap)).thenReturn(note);

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        assertDoesNotThrow(() -> migration.migrate(mockContext));

        verify(mockNoteService).importNote(note);
    }

    @Test
    void testMigrateFilesSuccessful() throws DecryptionFailedException {
        Map<String, Object> fileInfoMap = Map.of("id", "1");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("1");
        
        Map<String, Object> blockMap = Map.of("id", "block1");
        FileBlobInfo dataBlock = new FileBlobInfo();

        when(mockOldDbHelper.getFilesInfo()).thenReturn(List.of(fileInfoMap));
        when(mockEntityMapper.mapFileInfo(fileInfoMap)).thenReturn(fileInfo);
        when(mockOldDbHelper.getBlocksIdsByFileId("1")).thenReturn(List.of("block1"));
        when(mockOldDbHelper.getDataBlockById("block1")).thenReturn(blockMap);
        when(mockEntityMapper.mapFileBlobInfo(blockMap)).thenReturn(dataBlock);
        when(mockEntityMapper.getDataOfDataBlock(blockMap)).thenReturn(new byte[] {1, 2, 3});

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        assertDoesNotThrow(() -> migration.migrate(mockContext));

        verify(mockFileService).importFileInfo(fileInfo);
        verify(mockFileService).importFileBlobInfo(dataBlock);
    }

    @Test
    void testMigrateWithDecryptionFailure() throws DecryptionFailedException {
        Map<String, Object> noteMap = Map.of("id", "1");
        when(mockOldDbHelper.getAllNotes()).thenReturn(List.of(noteMap));
        when(mockEntityMapper.mapNote(noteMap))
                .thenThrow(new DecryptionFailedException(new Exception("Test error")));

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        AppMigrationException exception = assertThrows(AppMigrationException.class,
                () -> migration.migrate(mockContext));

        Throwable parrentException = exception.getCause();

        assertNotNull(parrentException);
        assertNotNull(parrentException.getMessage());
        assertTrue(parrentException.getMessage().contains("Failed to decrypt note"));
    }

    @Test
    void testWipeOldDbsSuccessful() throws IOException {
        File mockFile = mock(File.class);
        when(mockFilesUtils.getDatabaseFile(any(), any())).thenReturn(mockFile);

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        when(mockOldDbHelper.getAllNotes()).thenReturn(List.of());
        when(mockOldDbHelper.getFilesInfo()).thenReturn(List.of());

        assertDoesNotThrow(() -> migration.migrate(mockContext));

        verify(mockWiper, times(4)).wipeFile(any());
    }

    @Test
    void testWipeOldDbsWithIOException() throws IOException {
        File mockFile = mock(File.class);
        when(mockFilesUtils.getDatabaseFile(any(), any())).thenReturn(mockFile);
        doThrow(new IOException("Test error")).when(mockWiper).wipeFile(any());

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockDb).runInTransaction(any(Runnable.class));

        when(mockOldDbHelper.getAllNotes()).thenReturn(List.of());
        when(mockOldDbHelper.getFilesInfo()).thenReturn(List.of());

        AppMigrationException exception = assertThrows(AppMigrationException.class,
                () -> migration.migrate(mockContext));

        Throwable parrentException = exception.getCause();

        assertNotNull(parrentException);
        assertNotNull(parrentException.getMessage());
        assertTrue(parrentException.getMessage().contains("Failed to wipe file"));
    }
}
