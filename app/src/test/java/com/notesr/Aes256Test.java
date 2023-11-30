package com.notesr;

import com.peew.notesr.controllers.crypto.Aes256;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Assert;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.SecretKey;

public class Aes256Test {
    private static final String AVAILABLE_SYMBOLS = "0123456789qwertyuiopasdfghjklzxcvbnm";
    private static final Integer PASSWORD_LENGTH = 20;
    private static final byte[] PLAIN_DATA = new byte[4096];

    private static String password;

    @BeforeAll
    public static void beforeAll() {
        Random random = new Random();
        random.nextBytes(PLAIN_DATA);

        StringBuilder passwordBuilder = new StringBuilder();

        for(int i = 0; i < PASSWORD_LENGTH; i++) {
            passwordBuilder.append(AVAILABLE_SYMBOLS.charAt(random.nextInt(PASSWORD_LENGTH)));
        }

        password = passwordBuilder.toString();
    }

    @Test
    public void testEncryptionAndDecryptionWithKey() throws NoSuchAlgorithmException {
        SecretKey key = Aes256.generateRandomKey();
        byte[] salt = Aes256.generateRandomSalt();

        Aes256 aesInstance = new Aes256(key, salt);

        byte[] actualEncryptedData = aesInstance.encrypt(PLAIN_DATA);
        byte[] actualDecryptedData = aesInstance.decrypt(actualEncryptedData);

        Assert.assertArrayEquals(PLAIN_DATA, actualDecryptedData);
    }

    @Test
    public void testEncryptionAndDecryptionWithPassword() throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = Aes256.generateRandomSalt();
        Aes256 aesInstance = new Aes256(password, salt);

        byte[] actualEncryptedData = aesInstance.encrypt(PLAIN_DATA);
        byte[] actualDecryptedData = aesInstance.decrypt(actualEncryptedData);

        Assert.assertArrayEquals(PLAIN_DATA, actualDecryptedData);
    }
}
