package app.notesr.service.importer;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImportStatusCallback {
    private final Consumer<ImportStatus> onStatusUpdate;

    public void updateStatus(ImportStatus status) {
        onStatusUpdate.accept(status);
    }
}
