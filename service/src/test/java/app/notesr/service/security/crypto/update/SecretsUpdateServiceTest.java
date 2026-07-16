/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.crypto.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.util.TransactionalFilesUtil;
import app.notesr.data.AppDatabase;
import app.notesr.data.dao.FileBlobInfoDao;
import app.notesr.data.dao.FileInfoDao;
import app.notesr.data.dao.NoteDao;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;

@ExtendWith(MockitoExtension.class)
class SecretsUpdateServiceTest {

    private static final int KEY_SIZE = 48;

    @Mock
    private Context context;
    @Mock
    private DatabaseManager databaseManager;
    @Mock
    private TransactionalFilesUtil txFiles;
    @Mock
    private CryptoManager cryptoManager;
    @Mock
    private Consumer<SecretsUpdateState> onUpdate;

    private SecretsUpdateService secretsUpdateService;
    private SecretsUpdateStateHolder stateHolder;

    private final String dbName = "test.db";
    private final byte[] currentKey = new byte[KEY_SIZE];
    private final byte[] newKey = new byte[KEY_SIZE];
    private final char[] password = "password".toCharArray();

    @BeforeEach
    void setUp() {
        secretsUpdateService = new SecretsUpdateService(context, databaseManager);
        stateHolder = new SecretsUpdateStateHolder(onUpdate);

        // Initialize keys
        for (int i = 0; i < KEY_SIZE; i++) {
            currentKey[i] = (byte) i;
            newKey[i] = (byte) (i + 1);
        }
    }

    private CryptoSecrets createCurrentSecrets() {
        return new CryptoSecrets(currentKey.clone(), password.clone());
    }

    private CryptoSecrets createNewSecrets() {
        return new CryptoSecrets(newKey.clone(), password.clone());
    }

    @Test
    void testUpdateSecretsAlreadyDoneReturnsImmediately() throws Exception {
        when(cryptoManager.getSecrets()).thenReturn(createCurrentSecrets());
        stateHolder.setState(new SecretsUpdateState().setStatus(SecretsUpdateStatus.DONE));
        CryptoSecrets newSecrets = createNewSecrets();

        secretsUpdateService.updateSecrets(txFiles, cryptoManager, dbName, stateHolder, newSecrets);

        verify(databaseManager, never()).closeProvider();
        verify(txFiles, never()).commit();
        assertEquals(SecretsUpdateStatus.DONE, stateHolder.getState().getStatus(),
                "Status should remain DONE if already DONE");
    }

    @Test
    void testUpdateSecretsAlreadyFailedThrowsException() {
        when(cryptoManager.getSecrets()).thenReturn(createCurrentSecrets());
        stateHolder.setState(new SecretsUpdateState().setStatus(SecretsUpdateStatus.FAILED));
        CryptoSecrets newSecrets = createNewSecrets();

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw exception if status is already FAILED");
    }

    @Test
    void testUpdateSecretsSuccessfulUpdateFromStart() throws Exception {
        CryptoSecrets currentSecrets = createCurrentSecrets();
        CryptoSecrets newSecrets = createNewSecrets();

        when(cryptoManager.getSecrets()).thenReturn(currentSecrets);
        when(txFiles.isCommitted()).thenReturn(false);

        AppDatabase newDbMock = mock(AppDatabase.class);
        AppDatabase currentDbMock = mock(AppDatabase.class);
        AppDatabase tempDbMock = mock(AppDatabase.class);

        when(databaseManager.getDatabase(eq(dbName), any()))
                .thenReturn(newDbMock, currentDbMock);
        when(databaseManager.isDbAvailable(newDbMock)).thenReturn(false);

        File blobsDir = new File("blobs");
        when(txFiles.getInternalFile(eq(context), anyString())).thenReturn(blobsDir);

        File dbFile = new File("test.db");
        when(txFiles.getDatabaseFile(eq(context), eq(dbName))).thenReturn(dbFile);
        
        File stagedDbFile = mock(File.class);
        when(txFiles.stageFile(dbFile)).thenReturn(stagedDbFile);
        when(stagedDbFile.delete()).thenReturn(true);
        when(stagedDbFile.getAbsolutePath()).thenReturn("staged.db");
        
        when(databaseManager.getDatabase(eq("staged.db"), any()))
                .thenReturn(tempDbMock);

        File originalDbFile = new File("/data/user/0/app.notesr/databases/test.db");
        when(context.getDatabasePath(dbName)).thenReturn(originalDbFile);

        FileBlobInfoDao blobInfoDao = mock(FileBlobInfoDao.class);
        when(currentDbMock.getFileBlobInfoDao()).thenReturn(blobInfoDao);
        when(blobInfoDao.getAll()).thenReturn(Collections.emptyList());

        NoteDao noteDao = mock(NoteDao.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);
        when(currentDbMock.getNoteDao()).thenReturn(noteDao);
        when(currentDbMock.getFileInfoDao()).thenReturn(fileInfoDao);
        
        when(tempDbMock.getNoteDao()).thenReturn(noteDao);
        when(tempDbMock.getFileInfoDao()).thenReturn(fileInfoDao);
        when(tempDbMock.getFileBlobInfoDao()).thenReturn(blobInfoDao);

        doAnswer(invocation -> {
            Object argument = invocation.getArgument(0);

            if (argument instanceof Callable) {
                return ((Callable<?>) argument).call();
            } else if (argument instanceof Runnable) {
                ((Runnable) argument).run();
                return null;
            }

            return null;
        }).when(tempDbMock).runInTransaction(any(Callable.class));

        secretsUpdateService.updateSecrets(txFiles, cryptoManager, dbName, stateHolder, newSecrets);

        verify(databaseManager).closeProvider();
        verify(txFiles).commit();
        verify(cryptoManager).setSecrets(eq(context), any(CryptoSecrets.class));
        verify(databaseManager).reinitProvider(any());
        assertEquals(SecretsUpdateStatus.DONE, stateHolder.getState().getStatus(),
                "Status should be DONE after successful migration");
    }

    @Test
    void testUpdateSecretsAlreadyCommittedUpdatesStatusToDone() throws Exception {
        CryptoSecrets currentSecrets = createCurrentSecrets();
        CryptoSecrets newSecrets = createNewSecrets();

        stateHolder.setState(new SecretsUpdateState().setStatus(SecretsUpdateStatus.MOVING_DB_DATA));

        when(cryptoManager.getSecrets()).thenReturn(currentSecrets);
        when(txFiles.isCommitted()).thenReturn(true);

        secretsUpdateService.updateSecrets(txFiles, cryptoManager, dbName, stateHolder, newSecrets);

        verify(txFiles, never()).commit();
        verify(cryptoManager).setSecrets(eq(context), any(CryptoSecrets.class));
        verify(databaseManager).reinitProvider(any());

        assertEquals(SecretsUpdateStatus.DONE, stateHolder.getState().getStatus(),
                "Status should be DONE if transaction was already committed");
    }

    @Test
    void testUpdateSecretsMigrationFailureTriggersRollbackAndSetsFailed() {
        CryptoSecrets currentSecrets = createCurrentSecrets();
        CryptoSecrets newSecrets = createNewSecrets();

        when(cryptoManager.getSecrets()).thenReturn(currentSecrets);
        when(txFiles.isCommitted()).thenReturn(false);

        doThrow(new RuntimeException("Migration failed"))
                .when(txFiles).getInternalFile(any(), anyString());

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException and trigger rollback on migration failure");

        verify(txFiles).rollback();
        assertEquals(SecretsUpdateStatus.FAILED, stateHolder.getState().getStatus(),
                "Status should be FAILED after migration failure");
    }

    @Test
    void testMigrateDataDbAlreadyMigratedReturnsImmediately() throws Exception {
        AppDatabase newDb = mock(AppDatabase.class);
        when(databaseManager.getDatabase(dbName, newKey)).thenReturn(newDb);
        when(databaseManager.isDbAvailable(newDb)).thenReturn(true);

        secretsUpdateService.migrateData(txFiles, stateHolder, dbName, currentKey, newKey,
                new File("blobs"), mock(AesCryptor.class), mock(AesCryptor.class));

        verify(databaseManager, never()).getDatabase(dbName, currentKey);
    }

    @Test
    void testCopyDbDataCopiesAllDataSuccessfully() {
        AppDatabase currentDb = mock(AppDatabase.class);
        AppDatabase newDb = mock(AppDatabase.class);

        NoteDao noteDao = mock(NoteDao.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);
        FileBlobInfoDao fileBlobInfoDao = mock(FileBlobInfoDao.class);

        when(currentDb.getNoteDao()).thenReturn(noteDao);
        when(currentDb.getFileInfoDao()).thenReturn(fileInfoDao);
        when(currentDb.getFileBlobInfoDao()).thenReturn(fileBlobInfoDao);
        when(newDb.getNoteDao()).thenReturn(noteDao);
        when(newDb.getFileInfoDao()).thenReturn(fileInfoDao);
        when(newDb.getFileBlobInfoDao()).thenReturn(fileBlobInfoDao);

        List<Note> notes = Collections.singletonList(new Note());
        List<FileInfo> fileInfos = Collections.singletonList(new FileInfo());
        List<FileBlobInfo> blobInfos = Collections.singletonList(new FileBlobInfo());

        when(noteDao.getAll()).thenReturn(notes);
        when(fileInfoDao.getAll()).thenReturn(fileInfos);
        when(fileBlobInfoDao.getAll()).thenReturn(blobInfos);

        doAnswer(invocation -> {
            Object argument = invocation.getArgument(0);

            if (argument instanceof Callable) {
                return ((Callable<?>) argument).call();
            } else if (argument instanceof Runnable) {
                ((Runnable) argument).run();
                return null;
            }

            return null;
        }).when(newDb).runInTransaction(any(Callable.class));

        secretsUpdateService.copyDbData(currentDb, newDb);

        verify(noteDao).insertAll(notes);
        verify(fileInfoDao).insertAll(fileInfos);
        verify(fileBlobInfoDao).insertAll(blobInfos);
    }

    @Test
    void testUpdateBlobsDataMigratesBlobsSuccessfully() throws Exception {
        AppDatabase oldDb = mock(AppDatabase.class);
        FileBlobInfoDao blobInfoDao = mock(FileBlobInfoDao.class);
        when(oldDb.getFileBlobInfoDao()).thenReturn(blobInfoDao);

        FileBlobInfo blobInfo = new FileBlobInfo();
        blobInfo.setId("blob1");
        when(blobInfoDao.getAll()).thenReturn(List.of(blobInfo));

        File blobsDir = new File("blobs");
        byte[] oldData = "old".getBytes();
        byte[] decryptedData = "decrypted".getBytes();
        byte[] newData = "new".getBytes();

        AesCryptor currentCryptor = mock(AesCryptor.class);
        AesCryptor newCryptor = mock(AesCryptor.class);

        when(txFiles.readFileBytes(any())).thenReturn(oldData);
        when(currentCryptor.decrypt(oldData)).thenReturn(decryptedData);
        when(newCryptor.encrypt(decryptedData)).thenReturn(newData);

        secretsUpdateService.updateBlobsData(txFiles, oldDb, blobsDir, currentCryptor, newCryptor);

        verify(txFiles).writeFileBytes(any(), eq(newData));
    }

    @Test
    void testUpdateBlobsDataSkipsAlreadyMigratedBlob() throws Exception {
        AppDatabase oldDb = mock(AppDatabase.class);
        FileBlobInfoDao blobInfoDao = mock(FileBlobInfoDao.class);
        when(oldDb.getFileBlobInfoDao()).thenReturn(blobInfoDao);

        FileBlobInfo blobInfo = new FileBlobInfo();
        blobInfo.setId("blob1");
        when(blobInfoDao.getAll()).thenReturn(List.of(blobInfo));

        File blobsDir = new File("blobs");
        byte[] migratedData = "migrated".getBytes();

        AesCryptor currentCryptor = mock(AesCryptor.class);
        AesCryptor newCryptor = mock(AesCryptor.class);

        when(txFiles.isStaged(any())).thenReturn(true);
        when(txFiles.readFileBytes(any())).thenReturn(migratedData);
        // Successful decryption with new cryptor means it's migrated
        when(newCryptor.decrypt(migratedData)).thenReturn("plain".getBytes());

        secretsUpdateService.updateBlobsData(txFiles, oldDb, blobsDir, currentCryptor, newCryptor);

        verify(currentCryptor, never()).decrypt(any());
        verify(newCryptor, never()).encrypt(any());
        verify(txFiles, never()).writeFileBytes(any(), any());
    }

    @Test
    void testEncryptBlobDataWrapsGeneralSecurityException() throws Exception {
        AesCryptor cryptor = mock(AesCryptor.class);
        when(cryptor.encrypt(any())).thenThrow(new GeneralSecurityException("Encryption failed"));

        assertThrows(EncryptionFailedException.class, () -> secretsUpdateService.encryptBlobData(
                cryptor, new byte[0]),
                "Should wrap GeneralSecurityException in EncryptionFailedException during blob encryption");
    }

    @Test
    void testDecryptBlobDataWrapsGeneralSecurityException() throws Exception {
        AesCryptor cryptor = mock(AesCryptor.class);
        when(cryptor.decrypt(any())).thenThrow(new GeneralSecurityException("Decryption failed"));

        assertThrows(DecryptionFailedException.class, () -> secretsUpdateService.decryptBlobData(
                cryptor, new byte[0]),
                "Should wrap GeneralSecurityException in DecryptionFailedException during blob decryption");
    }

    @Test
    void testSetStatusUpdatesStateHolderAndTriggersOnUpdate() {
        secretsUpdateService.setStatus(stateHolder, SecretsUpdateStatus.MOVING_DB_DATA);

        assertEquals(SecretsUpdateStatus.MOVING_DB_DATA, stateHolder.getState().getStatus(),
                "Status should be updated in the state holder");
        verify(onUpdate).accept(any(SecretsUpdateState.class));
    }

    @Test
    void testGetStatusReturnsStatusFromStateHolder() {
        stateHolder.setState(new SecretsUpdateState()
                .setStatus(SecretsUpdateStatus.MOVING_BLOBS_DATA));

        SecretsUpdateStatus status = secretsUpdateService.getStatus(stateHolder);

        assertEquals(SecretsUpdateStatus.MOVING_BLOBS_DATA, status,
                "Should return the correct status from the state holder");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsKeyIsNull() {
        CryptoSecrets newSecrets = new CryptoSecrets(null, password.clone());

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets key is null");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsKeyIsEmpty() {
        CryptoSecrets newSecrets = new CryptoSecrets(new byte[0], password.clone());

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets key is empty");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsKeyWrongSize() {
        byte[] wrongSizedKey = new byte[32]; // Wrong size, should be 48
        CryptoSecrets newSecrets = new CryptoSecrets(wrongSizedKey, password.clone());

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets key has wrong size");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsKeyIsAllZeros() {
        byte[] nulledKey = new byte[KEY_SIZE]; // All zeros
        CryptoSecrets newSecrets = new CryptoSecrets(nulledKey, password.clone());

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets key is all zeros");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsPasswordIsNull() {
        CryptoSecrets newSecrets = new CryptoSecrets(newKey.clone(), null);

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets password is null");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsPasswordIsEmpty() {
        CryptoSecrets newSecrets = new CryptoSecrets(newKey.clone(), new char[0]);

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets password is empty");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsPasswordTooShort() {
        char[] shortPassword = "abc".toCharArray(); // Less than 4 characters
        CryptoSecrets newSecrets = new CryptoSecrets(newKey.clone(), shortPassword);

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets password is too short");
    }

    @Test
    void testUpdateSecretsThrowsWhenNewSecretsPasswordIsAllZeros() {
        char[] nulledPassword = new char[4]; // All '\0' characters
        CryptoSecrets newSecrets = new CryptoSecrets(newKey.clone(), nulledPassword);

        assertThrows(SecretsUpdateFailedException.class, () -> secretsUpdateService.updateSecrets(
                txFiles, cryptoManager, dbName, stateHolder, newSecrets),
                "Should throw SecretsUpdateFailedException when new secrets password is all zeros");
    }
}
