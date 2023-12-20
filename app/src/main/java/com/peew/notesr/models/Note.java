package com.peew.notesr.models;

public class Note {
    private final long id;
    private final String name;
    private final String text;

    public Note(long id, String name, String text) {
        this.id = id;
        this.name = name;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }
}
