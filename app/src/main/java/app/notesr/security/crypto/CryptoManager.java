package app.notesr.security.crypto;

import static app.notesr.util.HashHelper.fromSha256HexString;
import static app.notesr.util.HashHelper.toSha256Bytes;
import static app.notesr.util.HashHelper.toSha256String;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import app.notesr.App;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtils;
import app.notesr.util.Wiper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CryptoManager {
    public static final int KEY_SIZE = 48;
    private static final String TAG = CryptoManagerProvider.class.getName();
    private static final String KEY_HASH_PREF = "key_hash";
    private static final String BLOCK_MARKER_PREF = "is_blocked";
    private static final String ENCRYPTED_KEY_FILENAME = "key.encrypted";
    private static final String KEY_HASH_FILENAME = "key.sha256";
    private static final String BLOCK_MARKER_FILENAME = ".blocked";

    private final SharedPreferences prefs;

    private CryptoSecrets secrets;
    
    private Context getAppContext() {
        return App.getContext();
    }

    public boolean configure(String password) {
        try {
            this.secrets = tryGetSecretsWithFallback(password);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DecryptionFailedException e) {
            Log.e(TAG, "Key decryption failed", e);
            return false;
        }
    }

    public boolean isConfigured() {
        return secrets != null;
    }

    public boolean isKeyExists() {
        return getEncryptedKeyFile(getAppContext()).exists();
    }

    public boolean isBlocked() {
        boolean isBlocked = prefs.getBoolean(BLOCK_MARKER_PREF, false);
        return isBlocked || getBlockMarkerFile(getAppContext()).exists();
    }

    public CryptoSecrets generateSecrets(String password) {
        byte[] key = new byte[KEY_SIZE];
        new SecureRandom().nextBytes(key);

        return new CryptoSecrets(Arrays.copyOf(key, key.length), password);
    }

    public CryptoSecrets getSecrets() {
        return CryptoSecrets.from(secrets);
    }

    public void setSecrets(CryptoSecrets secrets) throws EncryptionFailedException {
        saveSecrets(secrets);
        this.secrets = secrets;
    }

    public boolean verifyKey(byte[] key) throws IOException,
            NoSuchAlgorithmException {
        byte[] originalHash = getKeyHash();

        if (originalHash != null) {
            byte[] providedHash = toSha256Bytes(key);
            return Arrays.equals(originalHash, providedHash);
        }

        return true;
    }

    public void block() throws IOException {
        Wiper.wipeFile(getEncryptedKeyFile(getAppContext()));
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, true).apply();
    }

    public void unblock() throws IOException {
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, false).apply();
        File blockMarkerFile = getBlockMarkerFile(getAppContext());

        if (blockMarkerFile.exists()) {
            Files.delete(blockMarkerFile.toPath());
        }
    }

    public void destroySecrets() {
        secrets = null;
    }

    private CryptoSecrets tryGetSecretsWithFallback(String password)
            throws IOException, DecryptionFailedException {
        try {
            return getSecrets(password, AesGcmCryptor.class);
        } catch (DecryptionFailedException e) {
            Log.e(TAG, "GCM decryption failed", e);
            return getSecrets(password, AesCbcCryptor.class);
        }
    }

    private void saveSecrets(CryptoSecrets secrets)
            throws EncryptionFailedException {
        try {
            byte[] keyHash = toSha256Bytes(secrets.getKey());
            byte[] encryptedKeyFileBytes = getKeyCryptor(secrets.getPassword(), AesGcmCryptor.class)
                    .encrypt(secrets.getKey());

            FilesUtils.writeFileBytes(getEncryptedKeyFile(getAppContext()), encryptedKeyFileBytes);
            setKeyHash(keyHash);
        } catch (Exception e) {
            throw new EncryptionFailedException(e);
        }
    }

    private CryptoSecrets getSecrets(String password, Class<? extends AesCryptor> cryptorClass)
            throws DecryptionFailedException {
        try {
            File keyFile = getEncryptedKeyFile(getAppContext());

            byte[] encryptedKeyFileBytes = FilesUtils.readFileBytes(keyFile);
            byte[] keyFileBytes = getKeyCryptor(password, cryptorClass)
                    .decrypt(encryptedKeyFileBytes);

            return new CryptoSecrets(Arrays.copyOf(keyFileBytes, keyFileBytes.length), password);
        } catch (Exception e) {
            throw new DecryptionFailedException(e);
        }
    }

    private AesCryptor getKeyCryptor(String password, Class<? extends AesCryptor> cryptorClass)
            throws NoSuchAlgorithmException {
        byte[] salt = AesCryptor.generatePasswordBasedSalt(password);

        try {
            return cryptorClass.getConstructor(String.class, byte[].class)
                    .newInstance(password, salt);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getKeyHash() throws IOException {
        String keyHash = prefs.getString(KEY_HASH_PREF, null);

        if (keyHash != null) {
            return fromSha256HexString(keyHash);
        }

        File keyHashFile = getKeyHashFile(getAppContext());

        if (keyHashFile.exists()) {
            return FilesUtils.readFileBytes(keyHashFile);
        }

        return null;
    }

    private void setKeyHash(byte[] keyHash) throws NoSuchAlgorithmException, IOException {
        prefs.edit().putString(KEY_HASH_PREF, toSha256String(keyHash)).apply();

        File keyHashFile = getKeyHashFile(getAppContext());

        if (keyHashFile.exists()) {
            Wiper.wipeFile(keyHashFile);
        }
    }

    private File getEncryptedKeyFile(Context context) {
        return FilesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME);
    }

    private File getBlockMarkerFile(Context context) {
        return FilesUtils.getInternalFile(context, BLOCK_MARKER_FILENAME);
    }

    private File getKeyHashFile(Context context) {
        return FilesUtils.getInternalFile(context, KEY_HASH_FILENAME);
    }
}
