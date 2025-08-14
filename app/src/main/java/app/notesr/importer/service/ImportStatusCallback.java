package app.notesr.importer.service;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportStatusCallback {
    private final Consumer<ImportStatus> onStatusUpdate;

    public void updateStatus(ImportStatus status) {
        onStatusUpdate.accept(status);
    }
}
