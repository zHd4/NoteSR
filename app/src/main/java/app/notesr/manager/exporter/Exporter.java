package app.notesr.manager.exporter;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
abstract class Exporter {
    @Getter(AccessLevel.PACKAGE)
    private final ExportThread thread;

    @Getter(AccessLevel.PACKAGE)
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
