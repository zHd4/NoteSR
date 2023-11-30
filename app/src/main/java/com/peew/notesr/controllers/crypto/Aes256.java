package com.peew.notesr.controllers.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes256 {
    private static final int KEY_SIZE = 256;
    private static final int SALT_SIZE = 16;
    private static final int DEFAULT_ITERATION_COUNT = 65536;
    private static final String MAIN_ALGORITHM = "AES";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256";

    private String password;
    private final SecretKey key;
    private final byte[] salt;

    public Aes256(SecretKey key, byte[] salt) {
        this.key = key;
        this.salt = salt;
    }

    public Aes256(String password, byte[] salt) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        this.password = password;
        this.salt = salt;
        this.key = generatePasswordBasedKey(password, salt);
    }

    public static SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(MAIN_ALGORITHM);
        generator.init(KEY_SIZE);

        return generator.generateKey();
    }

    private static SecretKey generatePasswordBasedKey(String password, byte[] salt) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec keySpec =
                new PBEKeySpec(password.toCharArray(), salt, DEFAULT_ITERATION_COUNT, KEY_SIZE);
        SecretKey pbeKey = SecretKeyFactory.getInstance(PBE_ALGORITHM).generateSecret(keySpec);

        return new SecretKeySpec(pbeKey.getEncoded(), MAIN_ALGORITHM);
    }

    public static byte[] generateRandomSalt() {
        byte[] result = new byte[SALT_SIZE];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(result);

        return result;
    }

    public byte[] encrypt(byte[] plainData)  {
        return new byte[0];
    }

    public byte[] decrypt(byte[] encryptedData)  {
        return new byte[0];
    }

    public String getPassword() {
        return password;
    }

    public SecretKey getKey() {
        return key;
    }

    public byte[] getSalt() {
        return salt;
    }
}
