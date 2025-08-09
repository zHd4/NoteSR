package app.notesr.importer.service;

import static java.util.UUID.randomUUID;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class IdAdapter {
    private final String id;

    public String getId() {
        try {
            Long.parseLong(id);
            return randomUUID().toString();
        } catch (NumberFormatException e) {
            return id;
        }
    }
}
