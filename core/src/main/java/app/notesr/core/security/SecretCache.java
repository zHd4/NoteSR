package app.notesr.core.security;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecretCache {
    private static final ConcurrentHashMap<String, byte[]> map = new ConcurrentHashMap<>();

    public static void put(String key, byte[] value) {
        map.put(key, value);
    }

    public static byte[] take(String key) {
        byte[] valueInMap = map.get(key);

        if (valueInMap == null) {
            return null;
        }

        byte[] value = Arrays.copyOf(valueInMap, valueInMap.length);

        Arrays.fill(valueInMap, (byte) 0);
        map.remove(key);

        return value;
    }

    public static void clear() {
        map.values().forEach(arr -> Arrays.fill(arr, (byte) 0));
        map.clear();
    }
}
