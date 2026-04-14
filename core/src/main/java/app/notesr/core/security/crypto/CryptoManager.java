/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import static app.notesr.core.util.HashUtils.fromSha256HexString;
import static app.notesr.core.util.HashUtils.toSha256Bytes;
import static app.notesr.core.util.HashUtils.toSha256String;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.security.exception.SessionExpiredException;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.WiperAdapter;
import lombok.RequiredArgsConstructor;

/**
 * Manages cryptographic operations, including key generation, storage, and session management.
 * This class handles the encryption and decryption of the master key used within the application.
 */
@RequiredArgsConstructor
public final class CryptoManager {

    /**
     * The size of the master key in bytes.
     */
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

    /**
     * Configures the CryptoManager by attempting to decrypt the stored master key with the provided password.
     *
     * @param context  The application context.
     * @param password The password to use for decryption.
     * @return {@code true} if configuration was successful, {@code false} if decryption failed.
     * @throws RuntimeException if an unexpected I/O error occurs.
     */
    public boolean configure(Context context, char[] password) {
        try {
            this.secrets = tryGetSecretsWithFallback(context, password);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DecryptionFailedException e) {
            return false;
        }
    }

    /**
     * Checks if the CryptoManager is currently configured with secrets in memory.
     *
     * @return {@code true} if configured, {@code false} otherwise.
     */
    public boolean isConfigured() {
        return secrets != null;
    }

    /**
     * Checks if the encrypted master key file exists on disk.
     *
     * @param context The application context.
     * @return {@code true} if the key file exists, {@code false} otherwise.
     */
    public boolean isKeyExists(Context context) {
        return filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME).exists();
    }

    /**
     * Checks if the application is currently marked as blocked.
     *
     * @param context The application context.
     * @return {@code true} if blocked, {@code false} otherwise.
     */
    public boolean isBlocked(Context context) {
        boolean isBlocked = prefs.getBoolean(BLOCK_MARKER_PREF, false);
        return isBlocked || filesUtils.getInternalFile(context, BLOCK_MARKER_FILENAME).exists();
    }

    /**
     * Generates a new set of {@link CryptoSecrets} with a randomly generated master key.
     *
     * @param password The password to associate with the new secrets.
     * @return A new {@link CryptoSecrets} instance.
     */
    public CryptoSecrets generateSecrets(char[] password) {
        byte[] key = new byte[KEY_SIZE];
        secureRandom.nextBytes(key);
        return new CryptoSecrets(Arrays.copyOf(key, key.length), password);
    }

    /**
     * Retrieves a copy of the current {@link CryptoSecrets}.
     *
     * @return A copy of the current secrets.
     * @throws SessionExpiredException if secrets are not configured or have expired.
     */
    public CryptoSecrets getSecrets() {
        CryptoSecrets secretsCopy = CryptoSecrets.from(secrets);

        if (secretsCopy == null) {
            throw new SessionExpiredException("Crypto secrets are not configured"
                    + " or session has expired");
        }

        return secretsCopy;
    }

    /**
     * Saves the provided secrets to disk and sets them as the current active secrets.
     *
     * @param context       The application context.
     * @param cryptoSecrets The secrets to save and set.
     * @throws EncryptionFailedException if saving the secrets fails.
     */
    public void setSecrets(Context context, CryptoSecrets cryptoSecrets)
            throws EncryptionFailedException {
        saveSecrets(context, cryptoSecrets);

        if (secrets != null) {
            secrets.destroy();
        }

        secrets = CryptoSecrets.from(cryptoSecrets);
    }

    /**
     * Verifies if the provided key matches the stored key hash.
     *
     * @param context The application context.
     * @param key     The key to verify.
     * @return {@code true} if the key is valid or no hash is stored, {@code false} otherwise.
     * @throws IOException              if an I/O error occurs while reading the hash.
     * @throws NoSuchAlgorithmException if the hashing algorithm is not available.
     */
    public boolean verifyKey(Context context, byte[] key)
            throws IOException, NoSuchAlgorithmException {
        byte[] originalHash = getKeyHash(context);
        if (originalHash != null) {
            byte[] providedHash = toSha256Bytes(key);
            return Arrays.equals(originalHash, providedHash);
        }
        return true;
    }

    /**
     * Blocks the application by destroying in-memory secrets, wiping the encrypted key file,
     * and setting a block marker.
     *
     * @param context The application context.
     * @throws IOException if an I/O error occurs during wiping or marking.
     */
    public void block(Context context) throws IOException {
        if (secrets != null) {
            secrets.destroy();
        }

        wiper.wipeFile(filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME));
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, true).apply();
    }

    /**
     * Unblocks the application by clearing the block marker and deleting the block marker file.
     *
     * @param context The application context.
     * @throws IOException if an I/O error occurs while deleting the block marker file.
     */
    public void unblock(Context context) throws IOException {
        prefs.edit().putBoolean(BLOCK_MARKER_PREF, false).apply();
        File blockMarkerFile = filesUtils.getInternalFile(context, BLOCK_MARKER_FILENAME);

        if (blockMarkerFile.exists()) {
            if (!blockMarkerFile.delete()) {
                throw new IOException("Failed to delete block marker file");
            }
        }
    }

    /**
     * Destroys the in-memory secrets and clears the reference.
     */
    public void destroySecrets() {
        if (secrets != null) {
            secrets.destroy();
            secrets = null;
        }
    }

    /**
     * Attempts to retrieve secrets using the modern AES-GCM cryptor, falling back to AES-CBC
     * for compatibility with older versions.
     *
     * @param context  The application context.
     * @param password The password for decryption.
     * @return The retrieved {@link CryptoSecrets}.
     * @throws IOException              if an I/O error occurs.
     * @throws DecryptionFailedException if decryption fails with both cryptors.
     */
    private CryptoSecrets tryGetSecretsWithFallback(Context context, char[] password)
            throws IOException, DecryptionFailedException {
        try {
            return getSecrets(context, password, AesGcmCryptor.class);
        } catch (DecryptionFailedException e) {
            return getSecrets(context, password, AesCbcCryptor.class);
        }
    }

    /**
     * Encrypts and saves the provided secrets to the internal storage.
     * Also updates the key hash in preferences.
     *
     * @param context       The application context.
     * @param cryptoSecrets The secrets to save.
     * @throws EncryptionFailedException if an error occurs during encryption or writing.
     */
    private void saveSecrets(Context context, CryptoSecrets cryptoSecrets)
            throws EncryptionFailedException {
        try {
            byte[] encryptedKeyFileBytes = cryptorFactory
                    .create(cryptoSecrets.getPassword(), AesGcmCryptor.class)
                    .encrypt(cryptoSecrets.getKey());

            File encryptedKeyFile = filesUtils.getInternalFile(context, ENCRYPTED_KEY_FILENAME);

            if (encryptedKeyFile.exists()) {
                wiper.wipeFile(encryptedKeyFile);
            }

            filesUtils.writeFileBytes(encryptedKeyFile, encryptedKeyFileBytes);
            setKeyHash(toSha256String(cryptoSecrets.getKey()), context);
            removeOldKeyHashFileIfExists(context);
        } catch (Exception e) {
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Decrypts the master key using the specified cryptor class.
     *
     * @param context      The application context.
     * @param password     The password for decryption.
     * @param cryptorClass The cryptor class to use (e.g., AesGcmCryptor).
     * @return The decrypted {@link CryptoSecrets}.
     * @throws DecryptionFailedException if decryption fails.
     */
    private CryptoSecrets getSecrets(
            Context context,
            char[] password,
            Class<? extends AesCryptor> cryptorClass
    ) throws DecryptionFailedException {

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

    /**
     * Retrieves the stored master key hash.
     *
     * @param context The application context.
     * @return The key hash as a byte array, or {@code null} if not found.
     * @throws IOException if an I/O error occurs.
     */
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

    /**
     * Stores the master key hash in SharedPreferences.
     *
     * @param keyHash The key hash string.
     */
    private void setKeyHash(String keyHash, Context context) throws IOException {
        prefs.edit().putString(KEY_HASH_PREF, keyHash).apply();
    }

    /**
     * Removes old key hash file that was in older versions of NoteSR
     * @param context application context
     * @throws IOException if failed to delete file
     */
    private void removeOldKeyHashFileIfExists(Context context) throws IOException {
        File keyHashFile = filesUtils.getInternalFile(context, KEY_HASH_FILENAME);

        if (keyHashFile.exists()) {
            wiper.wipeFile(keyHashFile);
        }
    }
}
