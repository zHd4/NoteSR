/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.notesr.core.security.exception.DecryptionFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class ValueDecryptorTest {

    @Mock
    private AesCryptor cryptor;

    private ValueDecryptor valueDecryptor;

    @BeforeEach
    void setUp() {
        valueDecryptor = new ValueDecryptor(cryptor);
    }

    @Test
    void testDecryptByteArraySuccess() throws Exception {
        byte[] encrypted = "encrypted".getBytes();
        byte[] decrypted = "decrypted".getBytes();

        when(cryptor.decrypt(encrypted)).thenReturn(decrypted);

        byte[] result = valueDecryptor.decrypt(encrypted);

        assertArrayEquals(decrypted, result,
                "Decrypted byte array should match the expected data");
    }

    @Test
    void testDecryptByteArrayThrowsDecryptionFailedException() throws Exception {
        byte[] encrypted = "encrypted".getBytes();

        when(cryptor.decrypt(any())).thenThrow(new GeneralSecurityException("Failed"));

        assertThrows(DecryptionFailedException.class, () -> valueDecryptor.decrypt(encrypted),
                "Decryption should throw DecryptionFailedException when cryptor fails");
    }

    @Test
    void testDecryptStringSuccess() throws Exception {
        String decryptedContent = "decrypted content";
        byte[] decryptedBytes = decryptedContent.getBytes();
        String base64Encrypted = Base64.getEncoder().encodeToString("encrypted".getBytes());

        when(cryptor.decrypt(any())).thenReturn(decryptedBytes);

        String result = valueDecryptor.decrypt(base64Encrypted);

        assertEquals(decryptedContent, result,
                "Decrypted string should match the original content");
    }

    @Test
    void testDecryptInvalidBase64StringThrowsDecryptionFailedException() {
        String invalidBase64 = "!!!NotBase64!!!";

        assertThrows(DecryptionFailedException.class, () -> valueDecryptor.decrypt(invalidBase64),
                "Should throw DecryptionFailedException for invalid Base64 input");
    }
}
