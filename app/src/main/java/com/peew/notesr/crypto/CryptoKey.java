package com.peew.notesr.crypto;

import javax.crypto.SecretKey;

public class CryptoKey {
    private SecretKey key;
    private byte[] salt;
    private String password;

    public SecretKey getKey() {
        return key;
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
