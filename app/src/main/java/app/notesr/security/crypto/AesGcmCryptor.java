package app.notesr.security.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AesGcmCryptor extends AesCryptor {
    private static final int CHUNK_SIZE = 100_000;
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int TAG_LENGTH = TAG_LENGTH_BIT / 8;
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
    public void encrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {

        byte[] iv = new byte[IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        out.write(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] inBuffer = new byte[CHUNK_SIZE];
        int bytesRead;

        while ((bytesRead = in.read(inBuffer)) != -1) {
            if (bytesRead == 0) {
                continue;
            }

            byte[] encryptedBlock = cipher.update(inBuffer, 0, bytesRead);

            if (encryptedBlock != null && encryptedBlock.length > 0) {
                out.write(encryptedBlock);
            }
        }

        byte[] finalBlock = cipher.doFinal();

        if (finalBlock != null && finalBlock.length > 0) {
            out.write(finalBlock);
        }

        out.flush();
    }

    @Override
    public void decrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {

        byte[] iv = readIvBytes(in);

        if (iv == null) {
            throw new IOException("Ciphertext too short: missing IV");
        }

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] inBuffer = new byte[CHUNK_SIZE];
        byte[] tail = new byte[TAG_LENGTH];

        int tailSize = 0;
        int readBytes;

        while ((readBytes = in.read(inBuffer)) != -1) {
            if (readBytes == 0) {
                continue;
            }

            int total = tailSize + readBytes;
            byte[] combined = new byte[total];

            if (tailSize > 0) {
                System.arraycopy(tail, 0, combined, 0, tailSize);
            }

            System.arraycopy(inBuffer, 0, combined, tailSize, readBytes);

            int toProcess = Math.max(0, total - TAG_LENGTH);

            if (toProcess > 0) {
                byte[] decryptedBlock = cipher.update(combined, 0, toProcess);

                if (decryptedBlock != null && decryptedBlock.length > 0) {
                    out.write(decryptedBlock);
                }
            }

            int newTailSize = Math.min(TAG_LENGTH, total);

            System.arraycopy(combined, total - newTailSize, tail, 0, newTailSize);
            tailSize = newTailSize;
        }

        if (tailSize < TAG_LENGTH) {
            throw new IOException("Ciphertext too short: missing GCM tag");
        }

        byte[] finalBlock = cipher.doFinal(tail, 0, TAG_LENGTH);

        if (finalBlock != null && finalBlock.length > 0) {
            out.write(finalBlock);
        }

        out.flush();
    }

    private static byte[] readIvBytes(InputStream in) throws IOException {
        byte[] buffer = new byte[IV_SIZE];
        int offset = 0;

        while (offset < IV_SIZE) {
            int bytesRead = in.read(buffer, offset, IV_SIZE - offset);

            if (bytesRead == -1) {
                return null;
            }

            offset += bytesRead;
        }

        return buffer;
    }
}
