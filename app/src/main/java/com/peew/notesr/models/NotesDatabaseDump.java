package com.peew.notesr.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NotesDatabaseDump(@JsonProperty("version") String version,
                                @JsonProperty("notes") List<Note> notes) {
}
