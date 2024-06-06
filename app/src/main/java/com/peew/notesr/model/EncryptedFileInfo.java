package com.peew.notesr.model;

public class EncryptedFileInfo {
    private Long id;

    private Long noteId;

    private Long size;

    private String encryptedName;

    private String encryptedType;

    public EncryptedFileInfo(Long id, Long noteId, Long size, String encryptedName,
                             String encryptedType) {
        this.id = id;
        this.noteId = noteId;
        this.size = size;
        this.encryptedName = encryptedName;
        this.encryptedType = encryptedType;
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

    public String getEncryptedName() {
        return encryptedName;
    }

    public void setEncryptedName(String encryptedName) {
        this.encryptedName = encryptedName;
    }

    public String getEncryptedType() {
        return encryptedType;
    }

    public void setEncryptedType(String encryptedType) {
        this.encryptedType = encryptedType;
    }
}
