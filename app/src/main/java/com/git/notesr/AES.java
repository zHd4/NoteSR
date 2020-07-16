package com.git.notesr;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    private static final int keySize = 128;
    private static final String cypherInstance = "AES/CBC/PKCS5Padding";
    private static final String secretKeyInstance = "PBKDF2WithHmacSHA1";
    private static final String initializationVector = "8119745113154120";

    public static String encrypt(String textToEncrypt, byte[] key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(cypherInstance);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                skeySpec,
                new IvParameterSpec(initializationVector.getBytes())
        );

        byte[] encrypted = cipher.doFinal(textToEncrypt.getBytes());

        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    public static String decrypt(String textToDecrypt, byte[] key) throws Exception {

        byte[] encryted_bytes = Base64.decode(textToDecrypt, Base64.DEFAULT);

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(cypherInstance);

        cipher.init(
                Cipher.DECRYPT_MODE,
                skeySpec,
                new IvParameterSpec(initializationVector.getBytes())
        );

        byte[] decrypted = cipher.doFinal(encryted_bytes);

        return new String(decrypted, StandardCharsets.UTF_8);
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