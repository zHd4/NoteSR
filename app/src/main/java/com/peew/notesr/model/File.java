package com.peew.notesr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public final class File {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("note_id")
    private Long noteId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("data")
    private byte[] data;

    public File() {}

    public File(String name,
                String type,
                Long size,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                byte[] data) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.data = data;
    }

    public File(Long noteId, String name, String type, Long size, byte[] data) {
        this.noteId = noteId;
        this.name = name;
        this.type = type;
        this.size = size;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
