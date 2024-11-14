package app.notesr.manager.exporter;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
abstract class Exporter {
    @Setter(AccessLevel.PACKAGE)
    private long total;

    @Setter(AccessLevel.PACKAGE)
    private ExportThread thread;

    private long exported;

    protected void increaseExported() {
        exported++;
    }

    abstract void export() throws IOException;
}
