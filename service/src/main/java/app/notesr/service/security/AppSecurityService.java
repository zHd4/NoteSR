/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.data.DatabaseProvider;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing application security operations including authentication
 * and access control.
 *
 * <p>This service provides a high-level API for security operations such as:
 * <ul>
 *   <li>Secret key generation and management</li>
 *   <li>Authentication</li>
 *   <li>App locking/blocking</li>
 *   <li>Stored key verification</li>
 *   <li>Session management</li>
 * </ul>
 *
 * <p>The service delegates operations to {@link CryptoManager} and wraps
 * low-level exceptions into higher-level security exceptions.
 *
 * @see CryptoManager
 * @see CryptoSecrets
 * @see AppSecurityException
 * @see AuthenticationFailedException
 */
@RequiredArgsConstructor
public final class AppSecurityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Context context;
    private final CryptoManager cryptoManager;

    /**
     * Constructs an AppSecurityService with the given context.
     *
     * <p>This constructor initializes the crypto manager using the singleton provider.
     *
     * @param context the Android context, used for file access and crypto initialization
     */
    public AppSecurityService(Context context) {
        this.context = context;
        this.cryptoManager = CryptoManagerProvider.getInstance(context);
    }

    /**
     * Generates new cryptographic secrets which contains randomly generated master key
     * and the provided password.
     * The key is absolutely random and not derived from the password.
     *
     * @param password the password to derive secrets from (will not be modified)
     * @return a new {@link CryptoSecrets} object containing derived key material
     */
    public CryptoSecrets getSecretsWithRandomKey(char[] password) {
        byte[] key = new byte[CryptoSecrets.MASTER_KEY_SIZE];
        SECURE_RANDOM.nextBytes(key);

        return new CryptoSecrets(key, password);
    }

    /**
     * Retrieves the currently configured cryptographic secrets.
     *
     * @return the current {@link CryptoSecrets} if configured, or null if not yet initialized
     * @see #isAuthConfigured()
     */
    public CryptoSecrets getActualSecrets() {
        return cryptoManager.getSecrets();
    }

    /**
     * Checks if the application is currently blocked.
     *
     * @return {@code true} if the app is blocked (locked), {@code false} otherwise
     * @see #blockApp()
     * @see #unblockApp(CryptoSecrets)
     */
    public boolean isAppBlocked() {
        return cryptoManager.isBlocked(context);
    }

    /**
     * Checks if authentication has been configured for the application.
     *
     * @return {@code true} if secrets have been set up, {@code false} if initial setup is required
     * @see #authenticate(char[])
     */
    public boolean isAuthConfigured() {
        return cryptoManager.isConfigured();
    }

    /**
     * Checks if a cryptographic key exists on the device.
     *
     * @return {@code true} if a key is stored, {@code false} otherwise
     */
    public boolean isKeyExists() {
        return cryptoManager.isKeyExists(context);
    }

    /**
     * Verifies if the provided key matches the key hash stored on the device.
     *
     * @param key the key bytes to verify against the stored hash
     * @return {@code true} if the key matches the stored hash, {@code false} otherwise
     * @throws AppSecurityException if the key hash file is not found, an I/O error occurs,
     *                              or the hash algorithm is not available
     */
    public boolean isKeyMatchingWithStored(byte[] key) {
        try {
            return cryptoManager.verifyKey(context, key);
        } catch (FileNotFoundException e) {
            throw new AppSecurityException("Failed to verify key, key hash not found", e);
        } catch (IOException e) {
            throw new AppSecurityException("Failed to verify key, I/O issue", e);
        } catch (NoSuchAlgorithmException e) {
            throw new AppSecurityException("Failed to verify key, hash algorithm issue", e);
        }
    }

    /**
     * Authenticates the application with the provided password.
     *
     * <p>This method verifies the password and decrypts the stored secrets. After successful
     * authentication, the app becomes accessible and secrets are cached in memory.
     *
     * @param password the password to authenticate with (will not be modified)
     * @throws AuthenticationFailedException if the password is invalid or decryption fails
     * @throws AppSecurityException if an I/O error occurs during authentication
     * @see #logout()
     */
    public void authenticate(char[] password) {
        try {
            cryptoManager.configure(context, password);
        } catch (DecryptionFailedException e) {
            throw new AuthenticationFailedException("Failed to authenticate, " +
                    "invalid password or cryptographic issue");
        } catch (IOException e) {
            throw new AppSecurityException("An I/O error occurred while authenticating", e);
        }
    }

    /**
     * Logs out the current user and clears all sensitive data.
     *
     * <p>This method performs the following operations:
     * <ul>
     *   <li>Closes database connections</li>
     *   <li>Destroys in-memory secrets</li>
     *   <li>Clears the secret cache</li>
     * </ul>
     *
     * <p>After logout, authentication is required to access the app again.
     *
     * @see #authenticate(char[])
     */
    public void logout() {
        DatabaseProvider.close();
        cryptoManager.destroySecrets();
        SecretCache.clear();
    }

    /**
     * Sets new cryptographic secrets and persists them securely.
     *
     * <p>After calling this method, the new secrets replace the existing ones and will be used
     * for encryption/decryption operations. The provided secrets object is destroyed after use
     * to prevent sensitive data leaks.
     *
     * @param newCryptoSecrets the new secrets to set and persist
     * @throws AppSecurityException if encryption fails during persistence
     */
    public void setSecrets(CryptoSecrets newCryptoSecrets) {
        try {
            cryptoManager.setSecrets(context, newCryptoSecrets);
        } catch (EncryptionFailedException e) {
            throw new AppSecurityException("Failed to apply new secrets, encryption issue", e);
        }

        newCryptoSecrets.destroy();
    }

    /**
     * Blocks the application, preventing further access until unblocked.
     *
     * <p>This is typically used for security purposes (e.g., after detecting suspicious activity).
     *
     * @throws AppSecurityException if an I/O error occurs during blocking
     * @see #unblockApp(CryptoSecrets)
     * @see #isAppBlocked()
     */
    public void blockApp() {
        try {
            cryptoManager.block(context);
        } catch (IOException e) {
            throw new AppSecurityException("Failed to block app, I/O issue", e);
        }
    }

    /**
     * Unblocks the application, restoring access.
     *
     * <p>This method verifies that the provided secrets are valid before unblocking. The key
     * from the secrets must match the stored key hash.
     *
     * @param cryptoSecrets the cryptographic secrets to verify and apply before unblocking
     * @throws AuthenticationFailedException if the key verification fails
     * @throws AppSecurityException if an I/O error or encryption error occurs during unblocking
     * @see #blockApp()
     */
    public void unblockApp(CryptoSecrets cryptoSecrets) {
        try {
            boolean isKeyValid = cryptoManager.verifyKey(context, cryptoSecrets.getKey());

            if (!isKeyValid) {
                throw new AuthenticationFailedException("Failed to unblock app, invalid key");
            }

            cryptoManager.setSecrets(context, cryptoSecrets);
            cryptoManager.unblock(context);
        } catch (IOException e) {
            throw new AppSecurityException("Failed to unblock app, I/O issue", e);
        } catch (EncryptionFailedException e) {
            throw new AppSecurityException("Failed to unblock app, encryption issue", e);
        } catch (NoSuchAlgorithmException e) {
            throw new AppSecurityException("Failed to unblock app, hash issue", e);
        }
    }
}
