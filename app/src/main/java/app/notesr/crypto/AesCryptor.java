package app.notesr.crypto;

import app.notesr.util.HashHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class AesCryptor {

    public enum AesMode {
        CBC("AES/CBC/PKCS5Padding"),
        GCM("AES/GCM/NoPadding");

        final String transformation;

        AesMode(String transformation) {
            this.transformation = transformation;
        }
    }

    public static final int KEY_SIZE = 256;
    public static final int SALT_SIZE = 16;
    public static final int IV_SIZE_CBC = 16;
    public static final int IV_SIZE_GCM = 12;
    private static final int DEFAULT_ITERATION_COUNT = 65536;
    public static final String KEY_GENERATOR_ALGORITHM = "AES";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey key;
    private final byte[] salt;
    private final AesMode mode;

    public AesCryptor(SecretKey key, byte[] salt, AesMode mode) {
        this.key = key;
        this.salt = salt;
        this.mode = mode;
    }

    public AesCryptor(String password, byte[] salt, AesMode mode)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.key = generatePasswordBasedKey(password, salt);
        this.salt = salt;
        this.mode = mode;
    }

    public static SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEY_GENERATOR_ALGORITHM);
        generator.init(KEY_SIZE);

        return generator.generateKey();
    }

    public static byte[] generateRandomSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] generatePasswordBasedSalt(String password) throws NoSuchAlgorithmException {
        byte[] passwordHash = HashHelper.toSha256Bytes(password.getBytes());
        return Arrays.copyOfRange(passwordHash, 0, SALT_SIZE);
    }

    private static SecretKey generatePasswordBasedKey(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec keySpec =
                new PBEKeySpec(password.toCharArray(), salt, DEFAULT_ITERATION_COUNT, KEY_SIZE);

        SecretKey secretKey = SecretKeyFactory.getInstance(PBE_ALGORITHM).generateSecret(keySpec);
        return new SecretKeySpec(secretKey.getEncoded(), KEY_GENERATOR_ALGORITHM);
    }

    public byte[] encrypt(byte[] plainData)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        byte[] iv = generateRandomIv();
        Cipher cipher = createCipher(key, iv, Cipher.ENCRYPT_MODE);

        byte[] encrypted = cipher.doFinal(plainData);
        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return result;
    }

    public byte[] decrypt(byte[] encryptedData)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        int ivLength = getIvLength();
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, ivLength);
        byte[] encrypted = Arrays.copyOfRange(encryptedData, ivLength, encryptedData.length);

        Cipher cipher = createCipher(key, iv, Cipher.DECRYPT_MODE);
        return cipher.doFinal(encrypted);
    }

    private Cipher createCipher(SecretKey key, byte[] iv, int mode)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(this.mode.transformation);
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), KEY_GENERATOR_ALGORITHM);

        if (this.mode == AesMode.GCM) {
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(mode, keySpec, gcmSpec);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(mode, keySpec, ivSpec);
        }

        return cipher;
    }

    private byte[] generateRandomIv() {
        byte[] iv = new byte[getIvLength()];
        new SecureRandom().nextBytes(iv);

        return iv;
    }

    private int getIvLength() {
        return (this.mode == AesMode.GCM) ? IV_SIZE_GCM : IV_SIZE_CBC;
    }
}
