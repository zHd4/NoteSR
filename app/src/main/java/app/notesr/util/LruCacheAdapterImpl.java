package app.notesr.util;

import android.util.LruCache;

import app.notesr.core.util.LruCacheAdapter;

public final class LruCacheAdapterImpl implements LruCacheAdapter {
    private final LruCache<Integer, byte[]> cache;

    public LruCacheAdapterImpl(int maxSize) {
        this.cache = new LruCache<>(maxSize);
    }

    @Override
    public byte[] get(int key) {
        return cache.get(key);
    }

    @Override
    public void put(int key, byte[] value) {
        cache.put(key, value);
    }
}
