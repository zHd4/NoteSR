/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import android.util.LruCache;

/**
 * Implementation of {@link LruCacheAdapter} using Android's {@link LruCache}.
 * This class provides a thread-safe LRU cache for byte arrays.
 */
public final class LruCacheAdapterImpl implements LruCacheAdapter {
    private final LruCache<Integer, byte[]> cache;

    public LruCacheAdapterImpl(int maxSize) {
        this.cache = new LruCache<>(maxSize);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte array associated with the key, or null if the key is not present
     */
    @Override
    public byte[] get(int key) {
        return cache.get(key);
    }

    /**
     * Associates the specified value with the specified key in the cache.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    @Override
    public void put(int key, byte[] value) {
        cache.put(key, value);
    }
}
