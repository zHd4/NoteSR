package com.peew.notesr.model;

public class EncryptedFile {
    private Long id;
    private Long noteId;
    private final String encryptedName;

    private final String encryptedType;
    private final byte[] encryptedData;

    public EncryptedFile(Long noteId, String encryptedType, String encryptedName, byte[] encryptedData) {
        this.noteId = noteId;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
        this.encryptedData = encryptedData;
    }

    public String getEncryptedName() {
        return encryptedName;
    }

    public String getEncryptedType() {
        return encryptedType;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getNoteId() {
        return noteId;
    }
}
