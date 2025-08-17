package app.notesr.exporter.service;

import java.util.function.Consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExportStatusHolder {
    private final Consumer<ExportStatus> onStatusUpdate;

    @Getter
    private ExportStatus status;

    public void setStatus(ExportStatus status) {
        this.status = status;
        onStatusUpdate.accept(status);
    }
}
