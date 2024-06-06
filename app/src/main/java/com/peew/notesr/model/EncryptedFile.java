package com.peew.notesr.model;

public class EncryptedFile {
    private Long id;
    private final Long noteId;
    private final String encryptedName;

    private final String encryptedType;
    private final Long size;
    private final byte[] encryptedData;

    public EncryptedFile(Long noteId, String encryptedName, String encryptedType, Long size,
                         byte[] encryptedData) {
        this.noteId = noteId;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
        this.size = size;
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

    public Long getSize() {
        return size;
    }
}
