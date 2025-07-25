package app.notesr.crypto;

import app.notesr.util.HashHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
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
    public static final int KEY_SIZE = 256;
    public static final int SALT_SIZE = 16;
    private static final int DEFAULT_ITERATION_COUNT = 65536;
    public static final String KEY_GENERATOR_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecretKey key;
    private final byte[] salt;

    public AesCryptor(SecretKey key, byte[] salt) {
        this.key = key;
        this.salt = salt;
    }

    public AesCryptor(String password, byte[] salt) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        this.salt = salt;
        this.key = generatePasswordBasedKey(password, salt);
    }

    public static Cipher createCipher(SecretKey key, byte[] iv, int mode) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), KEY_GENERATOR_ALGORITHM);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(mode, keySpec, ivSpec);

        return cipher;
    }

    public static SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEY_GENERATOR_ALGORITHM);
        generator.init(KEY_SIZE);

        return generator.generateKey();
    }

    public static byte[] generatePasswordBasedSalt(String password) throws
            NoSuchAlgorithmException {
        byte[] passwordHash = HashHelper.toSha256Bytes(password.getBytes());
        return Arrays.copyOfRange(passwordHash, 0, 16);
    }

    private static SecretKey generatePasswordBasedKey(String password, byte[] salt) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(),
                salt, DEFAULT_ITERATION_COUNT, KEY_SIZE);
        SecretKey pbeKey = SecretKeyFactory.getInstance(PBE_ALGORITHM).generateSecret(keySpec);

        return new SecretKeySpec(pbeKey.getEncoded(), CIPHER_ALGORITHM);
    }

    public static byte[] generateRandomSalt() {
        byte[] result = new byte[SALT_SIZE];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(result);

        return result;
    }

    public byte[] encrypt(byte[] plainData) throws
            NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return createCipher(key, salt, Cipher.ENCRYPT_MODE).doFinal(plainData);
    }

    public byte[] decrypt(byte[] encryptedData) throws
            NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        return createCipher(key, salt, Cipher.DECRYPT_MODE).doFinal(encryptedData);
    }
}
