package app.notesr.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AesCbcCryptor extends AesCryptor {
    public static final int IV_SIZE = 16;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKey key;
    private final byte[] iv;

    public AesCbcCryptor(String password, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.iv = iv;
        this.key = generatePasswordBasedKey(password, iv);
    }

    private static Cipher createCipher(SecretKey key, byte[] iv, int mode)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(mode, new SecretKeySpec(key.getEncoded(), "AES"),
                new IvParameterSpec(iv));

        return cipher;
    }

    @Override
    public byte[] encrypt(byte[] plainData) throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return createCipher(key, iv, Cipher.ENCRYPT_MODE).doFinal(plainData);
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return createCipher(key, iv, Cipher.DECRYPT_MODE).doFinal(encryptedData);
    }

    @Override
    public CipherOutputStream encrypt(OutputStream out) throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = createCipher(key, iv, Cipher.ENCRYPT_MODE);
        return new CipherOutputStream(out, cipher);
    }

    @Override
    public CipherInputStream decrypt(InputStream in) throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = createCipher(key, iv, Cipher.DECRYPT_MODE);
        return new CipherInputStream(in, cipher);
    }
}
