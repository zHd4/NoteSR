/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.util;

public interface LruCacheAdapter {
    byte[] get(int key);
    void put(int key, byte[] value);
}
