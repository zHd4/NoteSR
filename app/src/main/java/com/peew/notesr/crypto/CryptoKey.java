package com.peew.notesr.crypto;

import javax.crypto.SecretKey;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class CryptoKey {

    private SecretKey key;
    private byte[] salt;
    private String password;

    public CryptoKey copy() {
        return new CryptoKey(key, salt, password);
    }
}
