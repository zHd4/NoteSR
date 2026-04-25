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

import app.notesr.core.security.exception.EncryptionFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class ValueEncryptorTest {

    @Mock
    private AesCryptor cryptor;

    private ValueEncryptor valueEncryptor;

    @BeforeEach
    void setUp() {
        valueEncryptor = new ValueEncryptor(cryptor);
    }

    @Test
    void testEncryptByteArraySuccess() throws Exception {
        byte[] plain = "plain".getBytes();
        byte[] encrypted = "encrypted".getBytes();

        when(cryptor.encrypt(plain)).thenReturn(encrypted);

        byte[] result = valueEncryptor.encrypt(plain);

        assertArrayEquals(encrypted, result,
                "Encrypted byte array should match the expected data");
    }

    @Test
    void testEncryptByteArrayThrowsEncryptionFailedException() throws Exception {
        byte[] plain = "plain".getBytes();

        when(cryptor.encrypt(any())).thenThrow(new GeneralSecurityException("Failed"));

        assertThrows(EncryptionFailedException.class, () -> valueEncryptor.encrypt(plain),
                "Encryption should throw EncryptionFailedException when cryptor fails");
    }

    @Test
    void testEncryptStringSuccess() throws Exception {
        String plainContent = "plain content";
        byte[] encryptedBytes = "encrypted".getBytes();
        String expectedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);

        when(cryptor.encrypt(any())).thenReturn(encryptedBytes);

        String result = valueEncryptor.encrypt(plainContent);

        assertEquals(expectedBase64, result,
                "Encrypted string should be correctly Base64 encoded");
    }

    @Test
    void testEncryptStringThrowsEncryptionFailedException() throws Exception {
        String plainContent = "plain content";

        when(cryptor.encrypt(any())).thenThrow(new GeneralSecurityException("Failed"));

        assertThrows(EncryptionFailedException.class, () -> valueEncryptor.encrypt(plainContent),
                "Encryption should throw EncryptionFailedException"
                        + " when cryptor fails for string input");
    }
}
