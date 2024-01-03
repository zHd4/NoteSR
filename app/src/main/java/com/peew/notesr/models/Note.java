package com.peew.notesr.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Note(@JsonProperty("id") long id,
                   @JsonProperty("name") String name,
                   @JsonProperty("text") String text) {
}
