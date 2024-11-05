package app.notesr.crypto;

import android.util.Log;

import app.notesr.App;
import app.notesr.model.CryptoKey;
import app.notesr.tools.FileManager;
import app.notesr.tools.HashHelper;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Getter
public class CryptoManager {
    private static final String ENCRYPTED_KEY_FILENAME = "key.encrypted";
    private static final String HASHED_CRYPTO_KEY_FILENAME = "key.sha256";
    private static final String BLOCKED_FILENAME = ".blocked";
    private static final int KEY_BYTES_COUNT = Aes.KEY_SIZE / 8;
    private CryptoKey cryptoKeyInstance;

    public boolean configure(String password) {
        try {
            byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);
            Aes aesInstance = new Aes(password, secondarySalt);

            byte[] encryptedKeyFileBytes = FileManager.readFileBytes(getEncryptedKeyFile());
            byte[] keyFileBytes = aesInstance.decrypt(encryptedKeyFileBytes);

            byte[] mainKeyBytes = new byte[KEY_BYTES_COUNT];
            byte[] mainSaltBytes = new byte[Aes.SALT_SIZE];

            System.arraycopy(keyFileBytes, 0, mainKeyBytes, 0,KEY_BYTES_COUNT);
            System.arraycopy(keyFileBytes, KEY_BYTES_COUNT, mainSaltBytes, 0,Aes.SALT_SIZE);

            SecretKey mainKey = new SecretKeySpec(
                    mainKeyBytes,
                    0,
                    mainKeyBytes.length,
                    Aes.KEY_GENERATOR_ALGORITHM);

            cryptoKeyInstance = new CryptoKey(mainKey, mainSaltBytes, password);

            return true;
        } catch (Exception e) {
            Log.e("CryptoManager configuration error", e.toString());
            return false;
        }
    }

    public boolean ready() {
        return cryptoKeyInstance != null;
    }

    public boolean isFirstRun() {
        return !getEncryptedKeyFile().exists() && !getBlockFile().exists();
    }

    private File getEncryptedKeyFile() {
        return FileManager.getInternalFile(ENCRYPTED_KEY_FILENAME);
    }

    private File getBlockFile() {
        return FileManager.getInternalFile(BLOCKED_FILENAME);
    }

    private File getHashedCryptoKeyFile() {
        return FileManager.getInternalFile(HASHED_CRYPTO_KEY_FILENAME);
    }

    public CryptoKey generateNewKey(String password) throws NoSuchAlgorithmException {
        SecretKey mainKey = Aes.generateRandomKey();
        byte[] mainSalt = Aes.generateRandomSalt();

        return new CryptoKey(mainKey, mainSalt, password);
    }

    public CryptoKey createCryptoKey(byte[] keyBytes, byte[] salt, String password, boolean checkKey) throws
            Exception {
        SecretKey newKey = new SecretKeySpec(keyBytes, 0, keyBytes.length,
                Aes.KEY_GENERATOR_ALGORITHM);
        if (checkKey && !checkImportedKey(newKey, salt)) {
            throw new Exception("Wrong key");
        }

        return new CryptoKey(newKey, salt, password);
    }

    public void applyNewKey(CryptoKey newKey) throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, IOException {
        String password = newKey.getPassword();

        byte[] mainKey = newKey.getKey().getEncoded();
        byte[] mainSalt = newKey.getSalt();

        byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);
        byte[] keyFileData = new byte[KEY_BYTES_COUNT + Aes.SALT_SIZE];

        System.arraycopy(mainKey, 0, keyFileData, 0, mainKey.length);
        System.arraycopy(mainSalt, 0, keyFileData, mainKey.length, mainSalt.length);

        Aes aesInstance = new Aes(password, secondarySalt);
        FileManager.writeFileBytes(getEncryptedKeyFile(), aesInstance.encrypt(keyFileData));

        cryptoKeyInstance = newKey;
        File blockFile = getBlockFile();

        if (blockFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            blockFile.delete();
        }

        FileManager.writeFileBytes(getHashedCryptoKeyFile(), hashCryptoKeyData(mainKey, mainSalt));
    }

    public void changePassword(String newPassword) throws NoSuchAlgorithmException, IOException,
            InvalidKeySpecException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        File keyFile = getEncryptedKeyFile();
        String currentPassword = cryptoKeyInstance.getPassword();

        byte[] currentSecondarySalt = Aes.generatePasswordBasedSalt(currentPassword);
        byte[] newSecondarySalt = Aes.generatePasswordBasedSalt(newPassword);

        Aes aesInstance = new Aes(currentPassword, currentSecondarySalt);
        byte[] keyFileData = aesInstance.decrypt(FileManager.readFileBytes(keyFile));

        aesInstance = new Aes(newPassword, newSecondarySalt);
        FileManager.writeFileBytes(keyFile, aesInstance.encrypt(keyFileData));

        cryptoKeyInstance = new CryptoKey(
                cryptoKeyInstance.getKey(),
                cryptoKeyInstance.getSalt(),
                newPassword);
    }

    private byte[] hashCryptoKeyData(byte[] key, byte[] salt) throws NoSuchAlgorithmException {
        byte[] cryptoKeyBytes = new byte[key.length + salt.length];

        System.arraycopy(key, 0, cryptoKeyBytes, 0, key.length);
        System.arraycopy(salt, 0, cryptoKeyBytes, key.length, salt.length);

        return HashHelper.toSha256Bytes(cryptoKeyBytes);
    }

    private boolean checkImportedKey(SecretKey key, byte[] salt) {
        if (App.onAndroid()) {
            try {
                File hashedKeyFile = getHashedCryptoKeyFile();

                if (hashedKeyFile.exists()) {
                    byte[] originalCryptoKeyHash = FileManager.readFileBytes(hashedKeyFile);
                    byte[] hashedUserCryptoKey = hashCryptoKeyData(key.getEncoded(), salt);

                    return Arrays.equals(originalCryptoKeyHash, hashedUserCryptoKey);
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    public boolean isBlocked() {
        return getBlockFile().exists() && !getEncryptedKeyFile().exists();
    }

    public void block() {
        try {
            FileManager.writeFileBytes(getBlockFile(), new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroyKey() {
        cryptoKeyInstance = null;
    }
}
