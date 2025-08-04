package app.notesr.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AesGcmCryptor extends AesCryptor {
    public static final int IV_SIZE = 12;
    public static final int TAG_LENGTH_BIT = 128;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKey key;

    public AesGcmCryptor(String password, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        this.key = generatePasswordBasedKey(password, salt);
    }

    @Override
    public byte[] encrypt(byte[] plainData) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, IOException {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] encrypted = cipher.doFinal(plainData);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(iv);
        out.write(encrypted);

        return out.toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        if (encryptedData.length < IV_SIZE)
            throw new IllegalArgumentException("Data too short for GCM");

        byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_SIZE);
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_SIZE, encryptedData.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        return cipher.doFinal(ciphertext);
    }

    @Override
    public CipherOutputStream encrypt(OutputStream out) throws NoSuchAlgorithmException,
            IOException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        out.write(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return new CipherOutputStream(out, cipher);
    }

    @Override
    public CipherInputStream decrypt(InputStream in) throws IOException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] iv = new byte[IV_SIZE];
        int read = in.read(iv);
        if (read != IV_SIZE)
            throw new IOException("Could not read GCM IV");

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return new CipherInputStream(in, cipher);
    }
}
