/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.service.importer.v3;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.concurrent.Callable;


import app.notesr.data.AppDatabase;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;
import app.notesr.core.security.dto.CryptoSecrets;

@ExtendWith(MockitoExtension.class)
class ImportV3StrategyTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Mock
    private CryptoSecrets cryptoSecrets;

    @Mock
    private AppDatabase db;

    @Mock
    private NoteService noteService;

    @Mock
    private FileService fileService;

    @TempDir
    private File tempDir;

    private ImportV3Strategy strategy;

    @BeforeEach
    void setUp() throws IOException {
        File tempFile = new File(tempDir, "backup.zip");
        Files.write(tempFile.toPath(), "dummy".getBytes());

        byte[] testKeyBytes = new byte[48];
        RANDOM.nextBytes(testKeyBytes);

        when(cryptoSecrets.getKey()).thenReturn(testKeyBytes);
        when(db.runInTransaction(any(Callable.class))).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return callable.call();
        });

        strategy = spy(new ImportV3Strategy(cryptoSecrets, db, noteService, fileService, tempFile));
    }

    @Test
    void testImportDataIsCalledInsideTransaction() throws Exception {
        DataImporter importer = mock(DataImporter.class);
        doReturn(importer).when(strategy).getDataImporter(any());

        strategy.execute();

        verify(importer).importData();
    }

    @Test
    void testExceptionFromDataImporterIsPropagated() throws Exception {
        DataImporter importer = mock(DataImporter.class);
        doReturn(importer).when(strategy).getDataImporter(any());

        doThrow(new IOException("boom")).when(importer).importData();

        assertThrows(IOException.class, () -> strategy.execute());
    }
}
