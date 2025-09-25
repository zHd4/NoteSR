package app.notesr.importer.service.v3;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.concurrent.Callable;


import app.notesr.db.AppDatabase;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.ImportStatus;
import app.notesr.importer.service.ImportStatusCallback;
import app.notesr.note.service.NoteService;
import app.notesr.security.dto.CryptoSecrets;

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

    @Mock
    private ImportStatusCallback statusCallback;

    @TempDir
    private File tempDir;

    private File tempFile;
    private ImportV3Strategy strategy;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = new File(tempDir, "backup.zip");
        Files.write(tempFile.toPath(), "dummy".getBytes());

        byte[] testKeyBytes = new byte[48];
        RANDOM.nextBytes(testKeyBytes);

        when(cryptoSecrets.getKey()).thenReturn(testKeyBytes);
        when(db.runInTransaction(any(Callable.class))).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return callable.call();
        });

        strategy = spy(new ImportV3Strategy(
                cryptoSecrets, db, noteService, fileService, tempFile, statusCallback
        ));
    }

    @Test
    void testStatusUpdatedInCorrectOrder() {
        DataImporter importer = mock(DataImporter.class);
        doReturn(importer).when(strategy).getDataImporter(any());

        strategy.execute();

        InOrder inOrder = inOrder(statusCallback);
        inOrder.verify(statusCallback).updateStatus(ImportStatus.IMPORTING);
        inOrder.verify(statusCallback).updateStatus(ImportStatus.CLEANING_UP);
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

    @Test
    void testTempFileIsWipedAfterImport() {
        DataImporter importer = mock(DataImporter.class);
        doReturn(importer).when(strategy).getDataImporter(any());

        strategy.execute();

        assertTrue(tempFile.length() == 0 || !tempFile.exists(),
                "Temp file should be wiped or deleted after import");
    }
}
