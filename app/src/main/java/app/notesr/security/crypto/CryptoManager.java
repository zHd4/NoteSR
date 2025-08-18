package app.notesr.security.crypto;

import static app.notesr.util.HashUtils.fromSha256HexString;
import static app.notesr.util.HashUtils.toSha256Bytes;
import static app.notesr.util.HashUtils.toSha256String;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtilsAdapter;
import app.notesr.util.WiperAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CryptoManager {

    public static final int KEY_SIZE = 48;
    private static final String KEY_HASH_PREF = "key_hash";
    private static final String BLOCK_MARKER_PREF = "is_blocked";
    private static final String ENCRYPTED_KEY_FILENAME = "key.encrypted";
    private static final String KEY_HASH_FILENAME = "key.sha256";
    private static final String BLOCK_MARKER_FILENAME = ".blocked";

    private final SharedPreferences prefs;
    private final FilesUtilsAdapter filesUtils;
    private final WiperAdapter wiper;
    private final SecureRandom secureRandom;
    private final CryptorFactory cryptorFactory;

    private CryptoSecrets secrets;

    public boolean configure(Context context, String password) {
        try {
            this.secrets = tryGetSecretsWithFallback(context, password);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DecryptionFailedException e) {
            return false;
        }
    }

    public boolean isConfigured() {
        return secrets != null;
    }

    public boolean isKeyExists(Context context) {
        return filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME).exists();
    }

    public boolean isBlocked(Context context) {
        boolean isBlocked = prefs.getBoolean(BLOCK_MARKER_PREF, false);
        return isBlocked || filesUtils.getInternalFile(context, BLOCK_MARKER_FILENAME).exists();
    }

    public CryptoSecrets generateSecrets(String password) {
        byte[] key = new byte[KEY_SIZE];
        secureRandom.nextBytes(key);
        return new CryptoSecrets(Arrays.copyOf(key, key.length), password);
    }

    public CryptoSecrets getSecrets() {
        return CryptoSecrets.from(secrets);
    }

    public void setSecrets(Context context, CryptoSecrets secrets)
            throws EncryptionFailedException {
        saveSecrets(context, secrets);
        this.secrets = secrets;
    }

    public boolean verifyKey(Context context, byte[] key)
            throws IOException, NoSuchAlgorithmException {
        byte[] originalHash = getKeyHash(context);
        if (originalHash != null) {
            byte[] providedHash = toSha256Bytes(key);
            return Arrays.equals(originalHash, providedHash);
        }
        return true;
    }

    public void block(Context context) throws IOException {
        wiper.wipeFile(filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME));
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, true).apply();
    }

    public void unblock(Context context) throws IOException {
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, false).apply();
        File blockMarkerFile = filesUtils.getInternalFile(context, BLOCK_MARKER_FILENAME);

        if (blockMarkerFile.exists()) {
            if (!blockMarkerFile.delete()) {
                throw new IOException("Failed to delete block marker file");
            }
        }
    }

    public void destroySecrets() {
        secrets = null;
    }

    private CryptoSecrets tryGetSecretsWithFallback(Context context, String password)
            throws IOException, DecryptionFailedException {
        try {
            return getSecrets(context, password, AesGcmCryptor.class);
        } catch (DecryptionFailedException e) {
            return getSecrets(context, password, AesCbcCryptor.class);
        }
    }

    private void saveSecrets(Context context, CryptoSecrets secrets)
            throws EncryptionFailedException {
        try {
            byte[] keyHash = toSha256Bytes(secrets.getKey());
            byte[] encryptedKeyFileBytes = cryptorFactory
                    .create(secrets.getPassword(), AesGcmCryptor.class)
                    .encrypt(secrets.getKey());

            filesUtils.writeFileBytes(
                    filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME),
                    encryptedKeyFileBytes
            );
            setKeyHash(keyHash, context);
        } catch (Exception e) {
            throw new EncryptionFailedException(e);
        }
    }

    private CryptoSecrets getSecrets(Context context,
                                     String password,
                                     Class<? extends AesCryptor> cryptorClass)
            throws DecryptionFailedException {

        try {
            File keyFile = filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME);
            byte[] encryptedKeyFileBytes = filesUtils.readFileBytes(keyFile);
            byte[] keyFileBytes = cryptorFactory
                    .create(password, cryptorClass)
                    .decrypt(encryptedKeyFileBytes);
            return new CryptoSecrets(Arrays.copyOf(keyFileBytes, keyFileBytes.length), password);
        } catch (Exception e) {
            throw new DecryptionFailedException(e);
        }
    }

    private byte[] getKeyHash(Context context) throws IOException {
        String keyHash = prefs.getString(KEY_HASH_PREF, null);

        if (keyHash != null) {
            return fromSha256HexString(keyHash);
        }

        File keyHashFile = filesUtils.getInternalFile(context, KEY_HASH_FILENAME);

        if (keyHashFile.exists()) {
            return filesUtils.readFileBytes(keyHashFile);
        }

        return null;
    }

    private void setKeyHash(byte[] keyHash, Context context) throws
            NoSuchAlgorithmException, IOException {
        prefs.edit().putString(KEY_HASH_PREF, toSha256String(keyHash)).apply();
        File keyHashFile = filesUtils.getInternalFile(context, KEY_HASH_FILENAME);
        if (keyHashFile.exists()) {
            wiper.wipeFile(keyHashFile);
        }
    }
}
