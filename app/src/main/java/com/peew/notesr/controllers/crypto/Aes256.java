package com.peew.notesr.controllers.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Aes256 {
    public static final int KEY_SIZE = 256;
    public static final int SALT_SIZE = 16;

    private String password;

    private SecretKey key;

    private final byte[] salt;

    public Aes256(String password, byte[] salt) {
        this.password = password;
        this.salt = salt;
    }

    public Aes256(SecretKey key, byte[] salt) {
        this.key = key;
        this.salt = salt;
    }

    public static SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(KEY_SIZE);

        return generator.generateKey();
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
