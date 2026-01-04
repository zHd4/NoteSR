/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import static app.notesr.core.util.HashUtils.toSha256Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class AesCryptor {
    public static final int KEY_SIZE = 256;
    private static final int DEFAULT_ITERATION_COUNT = 65536;
    public static final String KEY_GENERATOR_ALGORITHM = "AES";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256";

    public static SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEY_GENERATOR_ALGORITHM);
        generator.init(KEY_SIZE);

        return generator.generateKey();
    }

    public static byte[] generatePasswordBasedSalt(char[] password)
            throws NoSuchAlgorithmException {
        byte[] passwordBytes = new String(password).getBytes();
        byte[] passwordHash = toSha256Bytes(passwordBytes);

        Arrays.fill(passwordBytes, (byte) 0);
        return Arrays.copyOfRange(passwordHash, 0, 16);
    }

    protected static SecretKey generatePasswordBasedKey(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, DEFAULT_ITERATION_COUNT,
                KEY_SIZE);

        SecretKey tmp = SecretKeyFactory.getInstance(PBE_ALGORITHM).generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public abstract byte[] encrypt(byte[] plainData)
            throws GeneralSecurityException;

    public abstract byte[] decrypt(byte[] encryptedData)
            throws GeneralSecurityException;

    public abstract void encrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException;

    public abstract void decrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException;
}
