package com.peew.notesr.model;

import java.time.LocalDateTime;

public class EncryptedFile {
    private Long id;
    private final Long noteId;
    private final String encryptedName;
    private final String encryptedType;
    private Long size;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private byte[] encryptedData;

    public EncryptedFile(Long noteId,
                         String encryptedName,
                         String encryptedType,
                         Long size,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         byte[] encryptedData) {
        this.noteId = noteId;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.encryptedData = encryptedData;
    }

    public EncryptedFile(Long id,
                         Long noteId,
                         String encryptedName,
                         String encryptedType,
                         Long size,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         byte[] encryptedData) {
        this.id = id;
        this.noteId = noteId;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.encryptedData = encryptedData;
    }

    public EncryptedFile(EncryptedFileInfo encryptedFileInfo) {
        this.id = encryptedFileInfo.getId();
        this.noteId = encryptedFileInfo.getNoteId();
        this.encryptedName = encryptedFileInfo.getEncryptedName();
        this.encryptedType = encryptedFileInfo.getEncryptedType();
        this.size = encryptedFileInfo.getSize();
        this.createdAt = encryptedFileInfo.getCreatedAt();
        this.updatedAt = encryptedFileInfo.getUpdatedAt();
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

    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
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

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
