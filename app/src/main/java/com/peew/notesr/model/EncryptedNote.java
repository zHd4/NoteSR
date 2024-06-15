package com.peew.notesr.model;

import java.time.LocalDateTime;

public final class EncryptedNote {
    private Long id;
    private final String encryptedName;
    private final String encryptedText;

    private LocalDateTime updatedAt;

    public EncryptedNote(String encryptedName, String encryptedText, LocalDateTime updatedAt) {
        this.encryptedName = encryptedName;
        this.encryptedText = encryptedText;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEncryptedName() {
        return encryptedName;
    }

    public String getEncryptedText() {
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
