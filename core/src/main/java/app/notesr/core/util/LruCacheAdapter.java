/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

/**
 * Interface for an LRU (Least Recently Used) cache adapter.
 * Provides basic get and put operations for byte array values indexed by integer keys.
 */
public interface LruCacheAdapter {
    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte array associated with the key, or null if the key is not present
     */
    byte[] get(int key);

    /**
     * Associates the specified value with the specified key in the cache.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    void put(int key, byte[] value);
}
