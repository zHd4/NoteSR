package com.peew.notesr.crypto;

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
}
