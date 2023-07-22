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

    public int getId() {
        return id;
    }

    private final int id;
    private String name;
    private String text;

    public Note(int id, String name, String text) {
        this.id = id;
        this.name = name;
        this.text = text;
    }
}
