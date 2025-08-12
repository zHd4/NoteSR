package app.notesr.security.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import static java.util.UUID.randomUUID;

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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import app.notesr.db.AppDatabase;
import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.security.dto.CryptoSecrets;
import io.bloco.faker.Faker;

@RunWith(AndroidJUnit4.class)
public class SecretsUpdateServiceIntegrationTest {
    private static final Faker FAKER = new Faker();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Context context;

    private String dbName;
    private CryptoSecrets newSecrets;
    private CryptoSecrets oldSecrets;
    private Note testNote;
    private FileInfo testFileInfo;
    private DataBlock testDataBlock;
    private SecretsUpdateService secretsUpdateService;

    @BeforeClass
    public static void beforeClass() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Before
    public void setUp() throws Exception {
        dbName = FAKER.name.name().replaceAll("\\W+", "") + ".db";

        byte[] oldKey = new byte[CryptoManager.KEY_SIZE];
        byte[] newKey = new byte[CryptoManager.KEY_SIZE];

        RANDOM.nextBytes(oldKey);
        RANDOM.nextBytes(newKey);

        oldSecrets = new CryptoSecrets(oldKey, FAKER.internet.password());

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        cryptoManager.setSecrets(context, oldSecrets);

        newSecrets = new CryptoSecrets(newKey, FAKER.internet.password());
        testNote = getTestNote();
        testFileInfo = getTestFileInfo(testNote);
        testDataBlock = getTestDataBlock(testFileInfo);
        secretsUpdateService = new SecretsUpdateService(context, dbName, cryptoManager, newSecrets);

        createTestDb(oldKey, testNote, testFileInfo, testDataBlock);
    }

    @Test
    public void testUpdateMigratesDataAndChangesKey() throws Exception {
        secretsUpdateService.update();

        AppDatabase dbWithNewKey = Room.databaseBuilder(context, AppDatabase.class, dbName)
                .openHelperFactory(new SupportFactory(newSecrets.getKey()))
                .build();

        List<Note> notes = dbWithNewKey.getNoteDao().getAll();
        List<FileInfo> files = dbWithNewKey.getFileInfoDao().getAll();
        List<DataBlock> dataBlocks = dbWithNewKey.getDataBlockDao().getAll();
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

        assertEquals(1, dataBlocks.size());
        assertEquals(testDataBlock.getId(), dataBlocks.get(0).getId());
        assertEquals(testDataBlock.getFileId(), dataBlocks.get(0).getFileId());
        assertEquals(testDataBlock.getOrder(), dataBlocks.get(0).getOrder());
        assertArrayEquals(testDataBlock.getData(), dataBlocks.get(0).getData());

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

    private void createTestDb(byte[] key, Note note, FileInfo fileInfo, DataBlock dataBlock) {
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, dbName)
                .openHelperFactory(new SupportFactory(Arrays.copyOf(key, key.length)))
                .build();

        db.getNoteDao().insert(note);
        db.getFileInfoDao().insert(fileInfo);
        db.getDataBlockDao().insert(dataBlock);

        db.close();
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

    private DataBlock getTestDataBlock(FileInfo fileInfo) {
        byte[] data = new byte[Math.toIntExact(fileInfo.getSize())];
        RANDOM.nextBytes(data);

        return new DataBlock(
                randomUUID().toString(),
                fileInfo.getId(),
                0L,
                data);
    }
}
