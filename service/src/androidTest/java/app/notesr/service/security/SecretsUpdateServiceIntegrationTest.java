/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import static java.util.UUID.randomUUID;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.sqlcipher.database.SupportFactory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.AppDatabase;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;
import app.notesr.service.file.FileService;
import io.bloco.faker.Faker;

@RunWith(AndroidJUnit4.class)
public class SecretsUpdateServiceIntegrationTest {
    private static final Faker FAKER = new Faker();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Context context;
    private static File blobsDir;

    private String dbName;
    private CryptoSecrets newSecrets;
    private CryptoSecrets oldSecrets;
    private Note testNote;
    private FileInfo testFileInfo;
    private FileBlobInfo testFileBlobInfo;
    private byte[] testFileData;
    private SecretsUpdateService secretsUpdateService;

    @BeforeClass
    public static void beforeClass() {
        context = ApplicationProvider.getApplicationContext();
        blobsDir = new File(context.getFilesDir(), FileService.BLOBS_DIR_NAME);
    }

    @Before
    public void setUp() throws Exception {
        dbName = FAKER.name.name().replaceAll("\\W+", "") + ".db";

        byte[] oldKey = new byte[CryptoManager.KEY_SIZE];
        byte[] newKey = new byte[CryptoManager.KEY_SIZE];

        RANDOM.nextBytes(oldKey);
        RANDOM.nextBytes(newKey);

        oldSecrets = new CryptoSecrets(oldKey, FAKER.internet.password().toCharArray());

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        cryptoManager.setSecrets(context, oldSecrets);

        FilesUtils filesUtils = new FilesUtils();

        newSecrets = new CryptoSecrets(newKey, FAKER.internet.password().toCharArray());
        testNote = getTestNote();
        testFileInfo = getTestFileInfo(testNote);
        testFileBlobInfo = getTestFileBlobInfo(testFileInfo);
        testFileData = getTestFileData(testFileInfo);
        secretsUpdateService = new SecretsUpdateService(context, dbName, cryptoManager, newSecrets,
                filesUtils);

        createTestDb(oldKey, testNote, testFileInfo, testFileBlobInfo, testFileData);
    }

    @Test
    public void testUpdateMigratesDataAndChangesKey() throws Exception {
        secretsUpdateService.update();

        byte[] newKey = newSecrets.getKey();
        AppDatabase dbWithNewKey = Room.databaseBuilder(context, AppDatabase.class, dbName)
                .openHelperFactory(new SupportFactory(Arrays.copyOf(newKey, newKey.length)))
                .build();

        List<Note> notes = dbWithNewKey.getNoteDao().getAll();
        List<FileInfo> files = dbWithNewKey.getFileInfoDao().getAll();
        List<FileBlobInfo> blobsInfo = dbWithNewKey.getFileBlobInfoDao().getAll();
        dbWithNewKey.close();

        assertEquals(1, notes.size());
        assertEquals(testNote.getId(), notes.get(0).getId());
        assertEquals(testNote.getName(), notes.get(0).getName());
        assertEquals(testNote.getText(), notes.get(0).getText());

        assertEquals(testNote.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS),
                notes.get(0).getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));

        assertEquals(1, files.size());
        assertEquals(testFileInfo.getId(), files.get(0).getId());
        assertEquals(testFileInfo.getNoteId(), files.get(0).getNoteId());
        assertEquals(testFileInfo.getName(), files.get(0).getName());
        assertEquals(testFileInfo.getSize(), files.get(0).getSize());

        assertEquals(testFileInfo.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                files.get(0).getCreatedAt().truncatedTo(ChronoUnit.SECONDS));

        assertEquals(testFileInfo.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS),
                files.get(0).getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));

        assertEquals(1, blobsInfo.size());
        assertEquals(testFileBlobInfo.getId(), blobsInfo.get(0).getId());
        assertEquals(testFileBlobInfo.getFileId(), blobsInfo.get(0).getFileId());
        assertEquals(testFileBlobInfo.getOrder(), blobsInfo.get(0).getOrder());

        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(newSecrets));
        File blobFile = new File(blobsDir, testFileBlobInfo.getId());

        byte[] decryptedData = cryptor.decrypt(Files.readAllBytes(blobFile.toPath()));
        assertArrayEquals(testFileData, decryptedData);

        try {
            AppDatabase dbWithOldKey = Room.databaseBuilder(context, AppDatabase.class, dbName)
                    .openHelperFactory(new SupportFactory(oldSecrets.getKey()))
                    .build();
            dbWithOldKey.getNoteDao().getAll();
            fail("Should be thrown exception when trying to use old key");
        } catch (Exception expected) {
            // Ok, database has been encrypted with a new key
        }

        File dbFile = context.getDatabasePath(dbName);

        assertFalse(context.getDatabasePath("tmp_" + dbName).exists());
        assertFalse(new File(dbFile.getAbsolutePath() + "-shm").exists());
        assertFalse(new File(dbFile.getAbsolutePath() + "-wal").exists());
    }

    private void createTestDb(byte[] key,
                              Note note,
                              FileInfo fileInfo,
                              FileBlobInfo fileBlobInfo,
                              byte[] fileBlobBytes) throws IOException, GeneralSecurityException {
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, dbName)
                .openHelperFactory(new SupportFactory(Arrays.copyOf(key, key.length)))
                .build();

        db.getNoteDao().insert(note);
        db.getFileInfoDao().insert(fileInfo);
        db.getFileBlobInfoDao().insert(fileBlobInfo);

        db.close();

        Files.createDirectories(blobsDir.toPath());

        AesGcmCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(oldSecrets));
        byte[] encryptedFileBlobBytes = cryptor.encrypt(fileBlobBytes);

        Files.write(blobsDir.toPath().resolve(fileBlobInfo.getId()), encryptedFileBlobBytes);
    }

    private Note getTestNote() {
        Note note = new Note();

        note.setId(randomUUID().toString());
        note.setName(FAKER.name.name());
        note.setText(FAKER.lorem.paragraph());
        note.setUpdatedAt(LocalDateTime.now());

        return note;
    }

    private FileInfo getTestFileInfo(Note note) {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setId(randomUUID().toString());
        fileInfo.setNoteId(note.getId());
        fileInfo.setName(FAKER.name.name().replaceAll("\\W+", ""));
        fileInfo.setSize((long) RANDOM.nextInt(10000));
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        return fileInfo;
    }

    private FileBlobInfo getTestFileBlobInfo(FileInfo fileInfo) {
        return new FileBlobInfo(randomUUID().toString(), fileInfo.getId(), 0L);
    }

    private byte[] getTestFileData(FileInfo fileInfo) {
        byte[] data = new byte[Math.toIntExact(fileInfo.getSize())];
        RANDOM.nextBytes(data);
        return data;
    }
}
