package com.peew.notesr.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record Note(@JsonProperty("id") long id,
                   @JsonProperty("name") String name,
                   @JsonProperty("text") String text) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return id == note.id && name.equals(note.name) && text.equals(note.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, text);
    }
}
