package com.peew.notesr.crypto;

import java.util.Arrays;
import java.util.Objects;

import javax.crypto.SecretKey;

public class CryptoKey {
    private final SecretKey key;
    private final byte[] salt;
    private final String password;

    public CryptoKey(SecretKey key, byte[] salt, String password) {
        this.key = key;
        this.salt = salt;
        this.password = password;
    }

    public SecretKey getKey() {
        return key;
    }

    public byte[] getSalt() {
        return salt;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CryptoKey cryptoKey = (CryptoKey) o;

        return key.equals(cryptoKey.key) && Arrays.equals(salt, cryptoKey.salt) &&
                password.equals(cryptoKey.password);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key, password);
        result = 31 * result + Arrays.hashCode(salt);

        return result;
    }
}
