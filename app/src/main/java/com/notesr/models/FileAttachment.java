package com.notesr.models;

public class FileAttachment {
    private final String name;

    private final byte[] data;

    public FileAttachment(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
