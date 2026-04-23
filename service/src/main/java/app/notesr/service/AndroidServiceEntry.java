/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a configuration entry for an Android {@link AndroidService}.
 * <p>
 * This class stores the service's class type, a human-readable name, and
 * an automatic startup preference. It provides utility methods for
 * serialization and deserialization using JSON.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class AndroidServiceEntry {

    /**
     * A human-readable identifier or display name for the service.
     */
    @JsonProperty(index = 1)
    private String serviceName;

    /**
     * The specific {@link AndroidService} class associated with this entry.
     */
    @JsonProperty(index = 2)
    private Class<? extends AndroidService> serviceClass;

    /**
     * The specific {@link AndroidServiceStarter} class used to launch the service.
     */
    @JsonProperty(index = 3)
    private Class<? extends AndroidServiceStarter> starterClass;

    /**
     * Indicates whether the service should be started automatically by the
     * {@link AndroidServiceBootstrapper} upon application initialization.
     */
    @JsonProperty(index = 4)
    private boolean autoStart;

    /**
     * Indicates whether the service requires user authorization before it
     * can be started or accessed.
     */
    @JsonProperty(index = 5)
    private boolean requiresAuth;

    /**
     * An optional JSON-formatted string or base64-encoded data payload passed to the
     * service during initialization.
     */
    @JsonProperty(index = 6)
    private String payload;

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
