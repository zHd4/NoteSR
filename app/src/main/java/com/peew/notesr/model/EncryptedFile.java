package com.peew.notesr.model;

public class EncryptedFile {
    private Long id;
    private final String encryptedName;
    private final byte[] encryptedData;

    public EncryptedFile(String encryptedName, byte[] encryptedData) {
        this.encryptedName = encryptedName;
        this.encryptedData = encryptedData;
    }

    public String getEncryptedName() {
        return encryptedName;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
