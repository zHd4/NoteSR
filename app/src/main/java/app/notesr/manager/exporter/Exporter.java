package app.notesr.manager.exporter;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
abstract class Exporter {
    @NonNull
    @Getter(AccessLevel.PROTECTED)
    private ExportThread thread;

    @Getter(AccessLevel.PACKAGE)
    private long exported;

    protected void increaseExported() {
        exported++;
    }

    protected void breakOnInterrupted() throws InterruptedException {
        if (thread.isInterrupted()) {
            throw new InterruptedException();
        }
    }

    abstract void export() throws IOException, InterruptedException;
    abstract long getTotal();
}
