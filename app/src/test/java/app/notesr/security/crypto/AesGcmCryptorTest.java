package app.notesr.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AesGcmCryptorTest {

    private static final String PASSWORD = "gcmPassword";
    private static final byte[] DATA = "GCM test string".getBytes();

    private AesGcmCryptor cryptor;

    @BeforeEach
    void setUp() throws Exception {
        byte[] salt = AesGcmCryptor.generatePasswordBasedSalt(PASSWORD);
        cryptor = new AesGcmCryptor(PASSWORD, salt);
    }

    @Test
    void testEncryptAndDecryptBytes() throws Exception {
        byte[] encrypted = cryptor.encrypt(DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(DATA, decrypted);
    }

    @Test
    void testEncryptAndDecryptStreams() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CipherOutputStream cos = cryptor.encrypt(out)) {
            cos.write(DATA);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try (CipherInputStream cis = cryptor.decrypt(in)) {
            cis.transferTo(result);
        }

        assertArrayEquals(DATA, result.toByteArray());
    }

    @Test
    void testEncryptShouldWriteIvToStream() throws Exception {
        OutputStream mockOut = mock(OutputStream.class);
        cryptor.encrypt(mockOut);

        verify(mockOut, times(1))
                .write(Mockito.argThat(iv -> iv.length == AesGcmCryptor.IV_SIZE));
    }

    @Test
    void testDecryptShouldThrowOnShortInput() {
        byte[] badData = Arrays.copyOf(DATA, AesGcmCryptor.IV_SIZE - 1);
        ByteArrayInputStream in = new ByteArrayInputStream(badData);

        assertThrows(IOException.class, () -> cryptor.decrypt(in));
    }
}
