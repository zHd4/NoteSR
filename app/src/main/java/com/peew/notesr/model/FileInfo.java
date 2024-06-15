package com.peew.notesr.model;

import java.time.LocalDateTime;

public class FileInfo {
    private Long id;

    private Long noteId;

    private Long size;

    private String name;

    private String type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public FileInfo(Long id,
                    Long noteId,
                    Long size,
                    String name,
                    String type,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.noteId = noteId;
        this.size = size;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public FileInfo(Long noteId, Long size, String name, String type) {
        this.noteId = noteId;
        this.size = size;
        this.name = name;
        this.type = type;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
