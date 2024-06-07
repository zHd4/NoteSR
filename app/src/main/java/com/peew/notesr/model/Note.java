package com.peew.notesr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public final class Note {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("text")
    private String text;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public Note() {}

    public Note(String name, String text, LocalDateTime updatedAt) {
        this.name = name;
        this.text = text;
        this.updatedAt = updatedAt;
    }

    public Note(String name, String text) {
        this.name = name;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
