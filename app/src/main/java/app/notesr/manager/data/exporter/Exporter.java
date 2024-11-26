package app.notesr.manager.data.exporter;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
abstract class Exporter {
    private final ExportThread thread;
    private long exported = 0;

    Exporter(ExportThread thread) {
        this.thread = thread;
    }

    protected void increaseExported() {
        exported++;
    }

    abstract void export() throws IOException, InterruptedException;
    abstract long getTotal();
}
