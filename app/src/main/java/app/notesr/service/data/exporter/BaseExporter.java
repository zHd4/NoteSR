package app.notesr.service.data.exporter;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
abstract class BaseExporter {
    private final ExportThread thread;
    private long exported = 0;

    BaseExporter(ExportThread thread) {
        this.thread = thread;
    }

    protected void increaseExported() {
        exported++;
    }

    abstract void export() throws IOException, InterruptedException;
    abstract long getTotal();
}
