package app.notesr.service.importer;

import static java.util.UUID.randomUUID;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class IdAdapter {
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
