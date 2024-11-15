package app.notesr.manager.exporter;

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

    protected void breakOnInterrupted() throws InterruptedException {
        if (thread.isInterrupted()) {
            throw new InterruptedException(thread.getClass().getName() + " interrupted");
        }
    }

    abstract void export() throws IOException, InterruptedException;
    abstract long getTotal();
}
