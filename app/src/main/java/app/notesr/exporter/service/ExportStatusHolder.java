package app.notesr.exporter.service;

import java.util.function.BiConsumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExportStatusHolder {

    private final BiConsumer<Integer, ExportStatus> onUpdate;

    @Getter
    private int progress;

    @Getter
    private ExportStatus status;

    public void setProgress(int progress) {
        this.progress = progress;
        onUpdate.accept(progress, status);
    }

    public void setStatus(ExportStatus status) {
        this.status = status;
        onUpdate.accept(progress, status);
    }
}
