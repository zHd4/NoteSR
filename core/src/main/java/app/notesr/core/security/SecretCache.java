/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A thread-safe cache for sensitive data (secrets) stored as byte arrays.
 * <p>
 * This class provides a centralized storage for secrets that need to be held in memory
 * temporarily. It emphasizes security by clearing the underlying byte arrays (filling with zeros)
 * when they are removed or taken from the cache to minimize the time sensitive data
 * remains in memory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecretCache {

    private static final ConcurrentHashMap<String, byte[]> MAP = new ConcurrentHashMap<>();

    /**
     * Checks if the cache contains a secret associated with the specified key.
     *
     * @param key the key to check
     * @return {@code true} if the key exists in the cache, {@code false} otherwise
     */
    public static boolean contains(String key) {
        return MAP.containsKey(key);
    }

    /**
     * Puts a secret into the cache.
     *
     * @param key   the key to associate the secret with
     * @param value the secret byte array to store
     */
    public static void put(String key, byte[] value) {
        MAP.put(key, value);
    }

    /**
     * Retrieves and removes a secret from the cache.
     * The cached copy is zeroed out before removal.
     *
     * @param key the key associated with the secret
     * @return a copy of the secret byte array, or {@code null} if the key is not found
     */
    public static byte[] take(String key) {
        byte[] valueInMap = MAP.get(key);

        if (valueInMap == null) {
            return null;
        }

        byte[] value = Arrays.copyOf(valueInMap, valueInMap.length);

        Arrays.fill(valueInMap, (byte) 0);
        MAP.remove(key);

        return value;
    }

    /**
     * Removes a secret from the cache if it exists, zeroing out the byte array.
     *
     * @param key the key to be removed
     */
    public static void removeIfExists(String key) {
        byte[] valueInMap = MAP.get(key);

        if (valueInMap != null) {
            Arrays.fill(valueInMap, (byte) 0);
            MAP.remove(key);
        }
    }

    /**
     * Clears the entire cache, zeroing out all stored byte arrays.
     */
    public static void clear() {
        MAP.values().forEach(arr -> Arrays.fill(arr, (byte) 0));
        MAP.clear();
    }
}
