package app.notesr.activity.file.viewer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.util.LruCacheAdapter;

import androidx.media3.common.C;
import androidx.media3.datasource.DataSpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.Uri;

class EncryptedMediaDataSourceTest {

    private AesCryptor cryptor;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        cryptor = mock(AesCryptor.class);
    }

    @Test
    void testConstructorThrowsIfCryptorIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new EncryptedMediaDataSource(null, List.of(new File("dummy")),
                        getLruCache(), 10));
    }

    @Test
    void testConstructorThrowsIfBlockFilesEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new EncryptedMediaDataSource(cryptor, List.of(), getLruCache(),
                        10));
    }

    @Test
    void testOpenThrowsIfPositionInvalid() throws IOException {
        File tempBlockFile = createTempBlock("b1", new byte[32]);
        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(), 16);

        DataSpec spec = new DataSpec.Builder().setUri(mockUri()).setPosition(9999).build();
        assertThrows(IOException.class, () -> dataSource.open(spec));
    }

    @Test
    void testReadReturnsDecryptedData() throws Exception {
        byte[] plainBlock = "Hello world!".getBytes();
        byte[] encryptedBlock = "dummyEncrypted".getBytes();
        File tempBlockFile = createTempBlock("b1", encryptedBlock);

        when(cryptor.decrypt(encryptedBlock)).thenReturn(plainBlock);

        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(),
                encryptedBlock.length - plainBlock.length);

        DataSpec spec = new DataSpec.Builder().setUri(mockUri()).setPosition(0).build();
        dataSource.open(spec);

        byte[] buffer = new byte[20];
        int bytesRead = dataSource.read(buffer, 0, buffer.length);

        assertEquals(plainBlock.length, bytesRead);
        assertArrayEquals(plainBlock, Arrays.copyOf(buffer, bytesRead));
    }

    @Test
    void testReadCachesBlocks() throws Exception {
        byte[] plainBlock = "ABCDEF".getBytes();
        byte[] encryptedBlock = "ENCRYPTED".getBytes();

        File tempBlockFile = createTempBlock("b1", encryptedBlock);
        when(cryptor.decrypt(encryptedBlock)).thenReturn(plainBlock);

        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(), 4);

        DataSpec spec = new DataSpec.Builder().setUri(mockUri()).build();
        dataSource.open(spec);

        byte[] buffer = new byte[plainBlock.length];
        dataSource.read(buffer, 0, buffer.length);

        dataSource.read(buffer, 0, buffer.length);
        verify(cryptor, times(1)).decrypt(encryptedBlock);
    }

    @Test
    void testReadHandlesEndOfInput() throws Exception {
        byte[] plainBlock = "xyz".getBytes();
        byte[] encryptedBlock = "ENC".getBytes();

        File tempBlockFile = createTempBlock("b1", encryptedBlock);
        when(cryptor.decrypt(encryptedBlock)).thenReturn(plainBlock);

        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(), 0);

        DataSpec spec = new DataSpec.Builder().setUri(mockUri()).setPosition(0).build();
        dataSource.open(spec);

        byte[] buffer = new byte[10];

        int firstBytesRead = dataSource.read(buffer, 0, 10);
        assertEquals(plainBlock.length, firstBytesRead);

        int secondBytesRead = dataSource.read(buffer, 0, 10);
        assertEquals(C.RESULT_END_OF_INPUT, secondBytesRead);
    }

    @Test
    void testGetUriReturnsCurrentUri() throws Exception {
        File tempBlockFile = createTempBlock("b1", new byte[32]);
        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(), 16);

        Uri expected = mockUri();

        DataSpec spec = new DataSpec.Builder().setUri(expected).setPosition(0).build();
        dataSource.open(spec);

        Uri actual = dataSource.getUri();

        assertNotNull(actual);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void testCloseResetsState() throws Exception {
        File tempBlockFile = createTempBlock("b1", new byte[32]);
        EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(cryptor,
                List.of(tempBlockFile), getLruCache(), 16);

        DataSpec spec = new DataSpec.Builder().setUri(mockUri()).setPosition(0).build();
        dataSource.open(spec);
        dataSource.close();

        assertNull(dataSource.getUri());

        byte[] buffer = new byte[10];
        assertThrows(IOException.class, () -> dataSource.read(buffer, 0, 5));
    }

    private File createTempBlock(String name, byte[] encryptedData) throws IOException {
        File tempFile = new File(tempDir.toFile(), name);

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(encryptedData);
        }

        return tempFile;
    }

    private Uri mockUri() {
        Uri fakeUri = mock(Uri.class);
        when(fakeUri.toString()).thenReturn("mock://test");

        return fakeUri;
    }

    private LruCacheAdapter getLruCache() {
        return new LruCacheAdapter() {
            private final Map<Integer, byte[]> map = new HashMap<>();

            @Override
            public byte[] get(int key) {
                return map.get(key);
            }

            @Override
            public void put(int key, byte[] value) {
                map.put(key, value);
            }
        };
    }
}