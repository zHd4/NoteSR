package app.notesr;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.notesr.crypto.AesCryptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.SecretKey;

class AesCryptorTest {

    private static final String PASSWORD = "SuperSecret123!";
    private static final byte[] SAMPLE_DATA = "Test data to encrypt".getBytes();

    private byte[] salt;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        salt = AesCryptor.generatePasswordBasedSalt(PASSWORD);
    }

    @Test
    void testEncryptDecryptWithPasswordCbcShouldMatchOriginal() throws Exception {
        AesCryptor cryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.CBC);

        byte[] encrypted = cryptor.encrypt(SAMPLE_DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(SAMPLE_DATA, decrypted);
    }

    @Test
    void testEncryptDecryptWithPasswordGcmShouldMatchOriginal() throws Exception {
        AesCryptor cryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.GCM);

        byte[] encrypted = cryptor.encrypt(SAMPLE_DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(SAMPLE_DATA, decrypted);
    }

    @Test
    void testEncryptDecryptWithGeneratedKeyCbcShouldMatchOriginal() throws Exception {
        SecretKey key = AesCryptor.generateRandomKey();
        AesCryptor cryptor = new AesCryptor(key, salt, AesCryptor.AesMode.CBC);

        byte[] encrypted = cryptor.encrypt(SAMPLE_DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(SAMPLE_DATA, decrypted);
    }

    @Test
    void testEncryptDecryptWithGeneratedKeyGcmShouldMatchOriginal() throws Exception {
        SecretKey key = AesCryptor.generateRandomKey();
        AesCryptor cryptor = new AesCryptor(key, salt, AesCryptor.AesMode.GCM);

        byte[] encrypted = cryptor.encrypt(SAMPLE_DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(SAMPLE_DATA, decrypted);
    }

    @Test
    void testDifferentIvShouldProduceDifferentCiphertexts() throws Exception {
        AesCryptor cryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.GCM);

        byte[] encrypted1 = cryptor.encrypt(SAMPLE_DATA);
        byte[] encrypted2 = cryptor.encrypt(SAMPLE_DATA);

        assertFalse(Arrays.equals(encrypted1, encrypted2),
                "Encrypted data should differ due to random IV");
    }

    @RepeatedTest(5)
    void testEncryptDecryptWithRandomKeyGcmShouldAlwaysSucceed() throws Exception {
        SecretKey key = AesCryptor.generateRandomKey();
        AesCryptor cryptor = new AesCryptor(key, salt, AesCryptor.AesMode.GCM);

        byte[] encrypted = cryptor.encrypt(SAMPLE_DATA);
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertArrayEquals(SAMPLE_DATA, decrypted);
    }

    @Test
    void testDecryptWithWrongKeyShouldFail() throws Exception {
        AesCryptor correctCryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.CBC);
        AesCryptor wrongCryptor = new AesCryptor("WrongPassword", salt,
                AesCryptor.AesMode.CBC);

        byte[] encrypted = correctCryptor.encrypt(SAMPLE_DATA);

        assertThrows(Exception.class, () -> wrongCryptor.decrypt(encrypted));
    }

    @Test
    void testDecryptWithWrongModeShouldFail() throws Exception {
        AesCryptor gcmCryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.GCM);
        AesCryptor cbcCryptor = new AesCryptor(PASSWORD, salt, AesCryptor.AesMode.CBC);

        byte[] encrypted = gcmCryptor.encrypt(SAMPLE_DATA);

        assertThrows(Exception.class, () -> cbcCryptor.decrypt(encrypted));
    }

    @Test
    void testGeneratePasswordBasedSaltShouldBeDeterministic() throws Exception {
        byte[] salt1 = AesCryptor.generatePasswordBasedSalt("pass123");
        byte[] salt2 = AesCryptor.generatePasswordBasedSalt("pass123");

        assertArrayEquals(salt1, salt2);
    }

    @Test
    void testGenerateRandomSaltShouldBeRandom() {
        byte[] salt1 = AesCryptor.generateRandomSalt();
        byte[] salt2 = AesCryptor.generateRandomSalt();

        assertFalse(Arrays.equals(salt1, salt2));
    }
}
