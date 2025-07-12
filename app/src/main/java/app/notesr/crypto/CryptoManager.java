package app.notesr.crypto;

import android.util.Log;

import app.notesr.App;
import app.notesr.dto.CryptoKey;
import app.notesr.util.FilesUtils;
import app.notesr.util.HashHelper;
import app.notesr.util.Wiper;
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
    private static final int KEY_BYTES_COUNT = AesCryptor.KEY_SIZE / 8;
    private CryptoKey cryptoKeyInstance;

    public boolean configure(String password) {
        try {
            byte[] secondarySalt = AesCryptor.generatePasswordBasedSalt(password);
            AesCryptor aesCryptor = new AesCryptor(password, secondarySalt);

            byte[] encryptedKeyFileBytes = FilesUtils.readFileBytes(getEncryptedKeyFile());
            byte[] keyFileBytes = aesCryptor.decrypt(encryptedKeyFileBytes);

            byte[] mainKeyBytes = new byte[KEY_BYTES_COUNT];
            byte[] mainSaltBytes = new byte[AesCryptor.SALT_SIZE];

            System.arraycopy(keyFileBytes, 0, mainKeyBytes, 0,KEY_BYTES_COUNT);
            System.arraycopy(keyFileBytes, KEY_BYTES_COUNT, mainSaltBytes, 0, AesCryptor.SALT_SIZE);

            SecretKey mainKey = new SecretKeySpec(
                    mainKeyBytes,
                    0,
                    mainKeyBytes.length,
                    AesCryptor.KEY_GENERATOR_ALGORITHM);

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
        return FilesUtils.getInternalFile(ENCRYPTED_KEY_FILENAME);
    }

    private File getBlockFile() {
        return FilesUtils.getInternalFile(BLOCKED_FILENAME);
    }

    private File getHashedCryptoKeyFile() {
        return FilesUtils.getInternalFile(HASHED_CRYPTO_KEY_FILENAME);
    }

    public CryptoKey generateNewKey(String password) throws NoSuchAlgorithmException {
        SecretKey mainKey = AesCryptor.generateRandomKey();
        byte[] mainSalt = AesCryptor.generateRandomSalt();

        return new CryptoKey(mainKey, mainSalt, password);
    }

    public CryptoKey createCryptoKey(byte[] keyBytes, byte[] salt, String password, boolean checkKey) throws
            Exception {
        SecretKey newKey = new SecretKeySpec(keyBytes, 0, keyBytes.length,
                AesCryptor.KEY_GENERATOR_ALGORITHM);
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

        byte[] secondarySalt = AesCryptor.generatePasswordBasedSalt(password);
        byte[] keyFileData = new byte[KEY_BYTES_COUNT + AesCryptor.SALT_SIZE];

        System.arraycopy(mainKey, 0, keyFileData, 0, mainKey.length);
        System.arraycopy(mainSalt, 0, keyFileData, mainKey.length, mainSalt.length);

        AesCryptor aesCryptor = new AesCryptor(password, secondarySalt);
        FilesUtils.writeFileBytes(getEncryptedKeyFile(), aesCryptor.encrypt(keyFileData));

        cryptoKeyInstance = newKey;
        File blockFile = getBlockFile();

        if (blockFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            blockFile.delete();
        }

        FilesUtils.writeFileBytes(getHashedCryptoKeyFile(), hashCryptoKeyData(mainKey, mainSalt));
    }

    public void changePassword(String newPassword) throws NoSuchAlgorithmException, IOException,
            InvalidKeySpecException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        File keyFile = getEncryptedKeyFile();
        String currentPassword = cryptoKeyInstance.getPassword();

        byte[] currentSecondarySalt = AesCryptor.generatePasswordBasedSalt(currentPassword);
        byte[] newSecondarySalt = AesCryptor.generatePasswordBasedSalt(newPassword);

        AesCryptor aesCryptor = new AesCryptor(currentPassword, currentSecondarySalt);
        byte[] keyFileData = aesCryptor.decrypt(FilesUtils.readFileBytes(keyFile));

        aesCryptor = new AesCryptor(newPassword, newSecondarySalt);
        FilesUtils.writeFileBytes(keyFile, aesCryptor.encrypt(keyFileData));

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
                    byte[] originalCryptoKeyHash = FilesUtils.readFileBytes(hashedKeyFile);
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
            Wiper.wipeFile(getEncryptedKeyFile());
            FilesUtils.writeFileBytes(getBlockFile(), new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroyKey() {
        cryptoKeyInstance = null;
    }
}
