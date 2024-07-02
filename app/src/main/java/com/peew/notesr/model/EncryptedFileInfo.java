package com.peew.notesr.model;

import java.time.LocalDateTime;

public class EncryptedFileInfo {
    private Long id;
    private Long noteId;
    private Long size;
    private byte[] encryptedName;
    private byte[] encryptedType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EncryptedFileInfo(Long id,
                             Long noteId,
                             Long size,
                             byte[] encryptedName,
                             byte[] encryptedType,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.noteId = noteId;
        this.size = size;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public byte[] getEncryptedName() {
        return encryptedName;
    }

    public void setEncryptedName(byte[] encryptedName) {
        this.encryptedName = encryptedName;
    }

    public byte[] getEncryptedType() {
        return encryptedType;
    }

    public void setEncryptedType(byte[] encryptedType) {
        this.encryptedType = encryptedType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
