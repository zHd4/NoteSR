package app.notesr.security.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AesCbcCryptor extends AesCryptor {
    private static final int CHUNK_SIZE = 100_000;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKey key;
    private final byte[] iv;

    public AesCbcCryptor(String password, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.iv = iv;
        this.key = generatePasswordBasedKey(password, iv);
    }

    private static Cipher createCipher(SecretKey key, byte[] iv, int mode)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(mode, new SecretKeySpec(key.getEncoded(), "AES"),
                new IvParameterSpec(iv));

        return cipher;
    }

    @Override
    public byte[] encrypt(byte[] plainData) throws GeneralSecurityException {
        return createCipher(key, iv, Cipher.ENCRYPT_MODE).doFinal(plainData);
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws GeneralSecurityException {
        return createCipher(key, iv, Cipher.DECRYPT_MODE).doFinal(encryptedData);
    }

    @Override
    public void encrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {

        Cipher cipher = createCipher(key, iv, Cipher.ENCRYPT_MODE);
        CipherOutputStream cos = new CipherOutputStream(out, cipher);

        try (in; cos) {
            byte[] chunk = new byte[CHUNK_SIZE];
            int bytesRead = in.read(chunk);

            while (bytesRead != -1) {
                if (bytesRead != CHUNK_SIZE) {
                    byte[] subChunk = new byte[bytesRead];
                    System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                    chunk = subChunk;
                }

                cos.write(chunk);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = in.read(chunk);
            }

            cos.flush();
        }
    }

    @Override
    public void decrypt(InputStream in, OutputStream out)
            throws GeneralSecurityException, IOException {
        Cipher cipher = createCipher(key, iv, Cipher.DECRYPT_MODE);
        CipherInputStream cis = new CipherInputStream(in, cipher);

        try (cis; out) {
            byte[] chunk = new byte[CHUNK_SIZE];
            int bytesRead = cis.read(chunk);

            while (bytesRead != -1) {
                if (bytesRead != CHUNK_SIZE) {
                    byte[] subChunk = new byte[bytesRead];
                    System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                    chunk = subChunk;
                }

                out.write(chunk);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = cis.read(chunk);
            }
        }
    }
}
