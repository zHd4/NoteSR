package app.notesr.security.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

class AesCbcCryptorTest {

    private static final String PASSWORD = "testPassword";
    private static final byte[] DATA = "CBC test data".getBytes();

    private AesCbcCryptor cryptor;

    @BeforeEach
    void setUp() throws Exception {
        byte[] iv = new byte[AesCbcCryptor.IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        cryptor = new AesCbcCryptor(PASSWORD, iv);
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

        try (CipherOutputStream cos = cryptor.getEncryptionStream(out)) {
            cos.write(DATA);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try (CipherInputStream cis = cryptor.getDecryptionStream(in)) {
            cis.transferTo(result);
        }

        assertArrayEquals(DATA, result.toByteArray());
    }
}
