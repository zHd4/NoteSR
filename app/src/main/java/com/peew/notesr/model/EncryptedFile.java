package com.peew.notesr.model;

import java.time.LocalDateTime;

public class EncryptedFile {
    private Long id;
    private final Long noteId;
    private final String encryptedName;

    private final String encryptedType;
    private final Long size;
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;
    private final byte[] encryptedData;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
