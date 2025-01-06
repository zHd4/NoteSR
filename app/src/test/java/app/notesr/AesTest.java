package app.notesr;

import app.notesr.crypto.Aes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AesTest extends TestBase {
    private static final String AVAILABLE_SYMBOLS = "0123456789qwertyuiopasdfghjklzxcvbnm";
    private static final int PASSWORD_LENGTH = 20;
    private static final int MIN_DATA_SIZE = 4096;
    private static final int MAX_DATA_SIZE = 10240;
    private static byte[] plainData;
    private static String password;

    @BeforeAll
    public static void beforeAll() {
        Random random = new Random();
        int plainDataSize = random.nextInt((MAX_DATA_SIZE - MIN_DATA_SIZE) + 1) +
                MIN_DATA_SIZE;

        plainData = new byte[plainDataSize];
        random.nextBytes(plainData);

        StringBuilder passwordBuilder = new StringBuilder();

        for(int i = 0; i < PASSWORD_LENGTH; i++) {
            passwordBuilder.append(AVAILABLE_SYMBOLS.charAt(random.nextInt(PASSWORD_LENGTH)));
        }

        password = passwordBuilder.toString();
    }

    @Test
    public void testEncryptionAndDecryptionWithKey() throws
            NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        SecretKey key = Aes.generateRandomKey();
        byte[] salt = Aes.generateRandomSalt();

        Aes aesInstance = new Aes(key, salt);

        byte[] actualEncryptedData = aesInstance.encrypt(plainData);
        byte[] actualDecryptedData = aesInstance.decrypt(actualEncryptedData);

        Assertions.assertArrayEquals(plainData, actualDecryptedData);
    }

    @Test
    public void testEncryptionAndDecryptionWithPassword() throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] salt = Aes.generateRandomSalt();
        Aes aesInstance = new Aes(password, salt);

        byte[] actualEncryptedData = aesInstance.encrypt(plainData);
        byte[] actualDecryptedData = aesInstance.decrypt(actualEncryptedData);

        Assertions.assertArrayEquals(plainData, actualDecryptedData);
    }
}
