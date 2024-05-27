package com.peew.notesr.model;

public final class EncryptedNote {
    private Long id;
    private final String encryptedName;
    private final String encryptedText;

    public EncryptedNote(String encryptedName, String encryptedText) {
        this.encryptedName = encryptedName;
        this.encryptedText = encryptedText;
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
}
