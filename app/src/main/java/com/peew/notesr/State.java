package com.peew.notesr;

import com.peew.notesr.models.CryptoKey;

public class State {
    private static final State INSTANCE = new State();
    private final CryptoKey cryptoKeyInstance = new CryptoKey();

    private State() {}

    public static State getInstance() {
        return INSTANCE;
    }

    public CryptoKey getCryptoKeyInstance() {
        return cryptoKeyInstance;
    }
}
