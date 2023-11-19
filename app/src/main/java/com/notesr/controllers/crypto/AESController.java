package com.notesr.controllers.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class AESController {

    private static final int keySize = 128;
    private static final String cypherInstance = "AES/CBC/PKCS5Padding";
    private static final String secretKeyInstance = "PBKDF2WithHmacSHA1";
    private static final String initializationVector = "8119745113154120";

    public static byte[] encrypt(final byte[] data, final byte[] key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(cypherInstance);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                skeySpec,
                new IvParameterSpec(initializationVector.getBytes())
        );

        return cipher.doFinal(data);
    }

    public static byte[] decrypt(final byte[] encrypted, final byte[] key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(cypherInstance);

        cipher.init(
                Cipher.DECRYPT_MODE,
                skeySpec,
                new IvParameterSpec(initializationVector.getBytes())
        );

        return cipher.doFinal(encrypted);
    }

    public static byte[] genKey(String plainText, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKeyInstance);
            KeySpec spec = new PBEKeySpec(
                    plainText.toCharArray(),
                    salt.getBytes(),
                    plainText.length(),
                    keySize
            );

            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }
}