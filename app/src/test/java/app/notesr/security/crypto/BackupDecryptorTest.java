package app.notesr.security.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.security.dto.CryptoSecrets;

class BackupDecryptorTest {

    private static final int KEY_LENGTH = 48;
    private static final int IV_LENGTH = 16;

    private ContentResolver contentResolver;
    private CryptoSecrets cryptoSecrets;
    private Uri inputUri;
    private BackupDecryptor decryptor;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        contentResolver = mock(ContentResolver.class);
        cryptoSecrets = mock(CryptoSecrets.class);
        inputUri = mock(Uri.class);

        File outputFile = tempDir.resolve("decrypted").toFile();
        decryptor = spy(new BackupDecryptor(contentResolver, cryptoSecrets, inputUri, outputFile));
    }

    @Test
    void testDecryptUsesGcmWhenSuccessful() throws Exception {
        InputStream mockStream = new ByteArrayInputStream("test".getBytes());
        when(contentResolver.openInputStream(inputUri)).thenReturn(mockStream);
        doNothing().when(decryptor).tryGcmDecryption(any(), any());

        decryptor.decrypt();

        verify(decryptor, times(1)).tryGcmDecryption(any(), any());
        verify(decryptor, never()).tryCbcDecryption(any(), any());
    }

    @Test
    void testDecryptFallsBackToCbcWhenGcmFails() throws Exception {
        InputStream mockStream1 = new ByteArrayInputStream("data1".getBytes());
        InputStream mockStream2 = new ByteArrayInputStream("data2".getBytes());
        when(contentResolver.openInputStream(inputUri)).thenReturn(mockStream1, mockStream2);

        doThrow(new GeneralSecurityException("fail"))
                .when(decryptor).tryGcmDecryption(any(), any());
        doNothing().when(decryptor).tryCbcDecryption(any(), any());

        try (MockedStatic<Log> logMock = mockStatic(Log.class)) {
            logMock.when(() -> Log.e(anyString(), anyString(), any())).thenReturn(0);

            decryptor.decrypt();

            logMock.verify(() ->
                    Log.e(anyString(), anyString(), any()), atLeast(0));
        }

        verify(decryptor, times(1)).tryGcmDecryption(any(), any());
        verify(decryptor, times(1)).tryCbcDecryption(any(), any());
    }

    @Test
    void testDecryptThrowsWhenBothGcmAndCbcFail() throws Exception {
        InputStream mockStream1 = new ByteArrayInputStream("abc".getBytes());
        InputStream mockStream2 = new ByteArrayInputStream("xyz".getBytes());
        when(contentResolver.openInputStream(inputUri)).thenReturn(mockStream1, mockStream2);

        doThrow(new GeneralSecurityException("fail1"))
                .when(decryptor).tryGcmDecryption(any(), any());
        doThrow(new GeneralSecurityException("fail2"))
                .when(decryptor).tryCbcDecryption(any(), any());

        try (MockedStatic<Log> logMock = mockStatic(Log.class)) {
            logMock.when(() -> Log.e(anyString(), anyString(), any())).thenReturn(0);

            assertThrows(DecryptionFailedException.class, () -> decryptor.decrypt());

            logMock.verify(() ->
                    Log.e(anyString(), anyString(), any()), atLeast(0));
        }
    }

    @Test
    void testGetIvExtractsIvCorrectly() {
        byte[] key = new byte[KEY_LENGTH];

        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }

        when(cryptoSecrets.getKey()).thenReturn(key);

        byte[] iv = decryptor.getIv(cryptoSecrets);

        assertEquals(IV_LENGTH, iv.length);
        assertArrayEquals(Arrays.copyOfRange(key, KEY_LENGTH - IV_LENGTH, KEY_LENGTH), iv);
    }
}