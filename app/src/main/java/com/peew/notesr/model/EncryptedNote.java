package com.peew.notesr.model;

import java.time.LocalDateTime;

public final class EncryptedNote {
    private Long id;
    private final byte[] encryptedName;
    private final byte[] encryptedText;

    private LocalDateTime updatedAt;

    public EncryptedNote(byte[] encryptedName, byte[] encryptedText, LocalDateTime updatedAt) {
        this.encryptedName = encryptedName;
        this.encryptedText = encryptedText;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public byte[] getEncryptedName() {
        return encryptedName;
    }

    public byte[] getEncryptedText() {
        return encryptedText;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
