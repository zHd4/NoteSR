package app.notesr.security.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AesGcmCryptor extends AesCryptor {
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
    public byte[] encrypt(byte[] plainData) throws GeneralSecurityException {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] encrypted = cipher.doFinal(plainData);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            out.write(iv);
            out.write(encrypted);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws GeneralSecurityException {
        if (encryptedData.length < IV_SIZE) {
            throw new IllegalArgumentException("Data too short for GCM");
        }

        byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_SIZE);
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_SIZE, encryptedData.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        return cipher.doFinal(ciphertext);
    }

    @Override
    public void encrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {

        byte[] inBuffer = new byte[CHUNK_SIZE];
        int bytesRead;

        while ((bytesRead = in.read(inBuffer)) != -1) {
            if (bytesRead == 0) {
                continue;
            }

            byte[] iv = new byte[IV_SIZE];

            SecureRandom.getInstanceStrong().nextBytes(iv);
            out.write(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encryptedBlock = cipher.doFinal(inBuffer, 0, bytesRead);
            out.write(encryptedBlock);
        }

        out.flush();
    }

    @Override
    public void decrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {

        byte[] ivBuffer = new byte[IV_SIZE];
        byte[] inBuffer = new byte[CHUNK_SIZE + TAG_LENGTH];

        while (true) {
            int ivRead = readFully(in, ivBuffer);

            if (ivRead == -1) {
                break;
            }

            if (ivRead < IV_SIZE) {
                throw new IOException("Ciphertext too short: missing IV");
            }

            int bytesRead = readBlock(in, inBuffer);
            if (bytesRead <= 0) {
                throw new IOException("Unexpected end of stream");
            }

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, ivBuffer));

            byte[] decryptedBlock = cipher.doFinal(inBuffer, 0, bytesRead);
            out.write(decryptedBlock);
        }

        out.flush();
    }

    private static int readBlock(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        int read;

        while ((read = in.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += read;
            if (offset == buffer.length) {
                break;
            }
        }
        return offset;
    }

    private static int readFully(InputStream in, byte[] buffer)
            throws IOException {
        final int offset = 0;
        int totalRead = 0;

        while (totalRead < IV_SIZE) {
            int read = in.read(buffer, offset + totalRead, IV_SIZE - totalRead);

            if (read == -1) {
                break;
            }

            totalRead += read;
        }

        return totalRead == 0 ? -1 : totalRead;
    }
}
