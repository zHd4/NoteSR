package app.notesr.manager.exporter;

import java.io.IOException;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
abstract class Exporter {
    @NonNull
    private ExportThread thread;

    private long exported;

    protected void increaseExported() {
        exported++;
    }

    abstract void export() throws IOException, InterruptedException;
    abstract long getTotal();
}
