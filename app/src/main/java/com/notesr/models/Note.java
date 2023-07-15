package com.notesr.models;

public class Note {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private String name;
    private String text;

    public Note(String name, String text) {
        this.name = name;
        this.text = text;
    }
}
