/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer.v3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.crypto.AesCryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;


@ExtendWith(MockitoExtension.class)
class BackupDecryptorTest {

    @Mock
    private AesCryptor cryptor;

    private BackupDecryptor decryptor;

    @BeforeEach
    void setUp() {
        decryptor = new BackupDecryptor(cryptor);
    }

    @Test
    void testDecryptShouldReturnDecryptedDataWhenSuccessful() throws Exception {
        byte[] encrypted = "encrypted".getBytes();
        byte[] decrypted = "decrypted".getBytes();
        when(cryptor.decrypt(encrypted)).thenReturn(decrypted);

        byte[] result = decryptor.decrypt(encrypted);

        assertArrayEquals(decrypted, result);
        verify(cryptor).decrypt(encrypted);
    }

    @Test
    void testDecryptShouldThrowDecryptionFailedExceptionWhenDecryptionFails() throws Exception {
        byte[] encrypted = "encrypted".getBytes();
        when(cryptor.decrypt(encrypted))
                .thenThrow(new GeneralSecurityException("Decryption failed"));

        assertThrows(DecryptionFailedException.class, () -> decryptor.decrypt(encrypted));
        verify(cryptor).decrypt(encrypted);
    }

    @Test
    void testDecryptJsonObjectShouldReturnDecryptedStringWhenSuccessful() throws Exception {
        byte[] encrypted = "encrypted".getBytes();
        byte[] decrypted = "{\"key\": \"value\"}".getBytes();
        when(cryptor.decrypt(encrypted)).thenReturn(decrypted);

        String result = decryptor.decryptJsonObject(encrypted);

        assertEquals(new String(decrypted), result);
        verify(cryptor).decrypt(encrypted);
    }

    @Test
    void testDecryptJsonObjectShouldThrowDecryptionFailedExceptionWhenDecryptionFails()
            throws Exception {

        byte[] encrypted = "encrypted".getBytes();
        when(cryptor.decrypt(encrypted))
                .thenThrow(new GeneralSecurityException("Decryption failed"));

        assertThrows(DecryptionFailedException.class, () -> decryptor.decryptJsonObject(encrypted));
        verify(cryptor).decrypt(encrypted);
    }
}
