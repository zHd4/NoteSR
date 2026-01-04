/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class SecretCacheTest {
    @AfterEach
    void tearDown() {
        SecretCache.clear();
    }

    @Test
    void testPutAndTakeValueSuccessfully() {
        byte[] secret = new byte[]{1, 2, 3};
        SecretCache.put("key1", Arrays.copyOf(secret, secret.length));

        byte[] retrieved = SecretCache.take("key1");
        assertArrayEquals(secret, retrieved,
                "The retrieved value should match the value that was put in the cache");

        byte[] afterRemoval = SecretCache.take("key1");
        assertNull(afterRemoval,
                "After removal, taking the same key should return null");
    }

    @Test
    void testTakeNonExistingKeyReturnsNull() {
        byte[] value = SecretCache.take("nonexistent");
        assertNull(value,
                "Taking a key that does not exist should return null");
    }

    @Test
    void testClearEmptiesCacheAndZeroesValues() {
        byte[] secret1 = new byte[]{4, 5, 6};
        byte[] secret2 = new byte[]{7, 8, 9};

        SecretCache.put("key1", secret1);
        SecretCache.put("key2", secret2);

        SecretCache.clear();

        assertNull(SecretCache.take("key1"), "After clear, key1 should return null");
        assertNull(SecretCache.take("key2"), "After clear, key2 should return null");

        assertArrayEquals(new byte[]{0,0,0}, secret1,
                "Array for key1 should be zeroed after clear");
        assertArrayEquals(new byte[]{0,0,0}, secret2,
                "Array for key2 should be zeroed after clear");
    }

    @Test
    void testOverwriteValueWithSameKey() {
        byte[] first = new byte[]{10};
        byte[] second = new byte[]{20};

        SecretCache.put("key", Arrays.copyOf(first, first.length));
        SecretCache.put("key", Arrays.copyOf(second, second.length));

        byte[] retrieved = SecretCache.take("key");
        assertArrayEquals(second, retrieved,
                "The last value put with the same key should overwrite the previous value");
    }
}
