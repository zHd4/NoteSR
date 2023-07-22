package com.notesr.models;

public class FileAttachment {
    private final int id;
    private final int noteId;
    private final String name;

    private byte[] data;

    public FileAttachment(int id, int noteId, String name, byte[] data) {
        this.id = id;
        this.noteId = noteId;
        this.name = name;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public int getNoteId() {
        return noteId;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
