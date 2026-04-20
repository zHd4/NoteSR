/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a configuration entry for an Android {@link Service}.
 * <p>
 * This class stores the service's class type, a human-readable name, and
 * an automatic startup preference. It provides utility methods for
 * serialization and deserialization using JSON.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AndroidServiceEntry {

    /**
     * The specific Android {@link Service} class associated with this entry.
     */
    @JsonProperty(index = 1)
    private Class<? extends Service> serviceClass;

    /**
     * A human-readable identifier or display name for the service.
     */
    @JsonProperty(index = 2)
    private String serviceName;

    /**
     * Indicates whether the service should be started automatically
     * by the application
     */
    @JsonProperty(index = 3)
    private boolean autoStart;

    /**
     * Deserializes a JSON string into an {@code AndroidServiceEntry} instance.
     *
     * @param json the JSON string representing the service entry.
     * @return a new instance of {@code AndroidServiceEntry}.
     * @throws JsonProcessingException if the JSON string is malformed or incompatible.
     */
    public static AndroidServiceEntry fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, AndroidServiceEntry.class);
    }

    /**
     * Serializes the current service entry instance into a JSON string.
     *
     * @return a JSON string representation of this object.
     * @throws JsonProcessingException if the object cannot be serialized.
     */
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
