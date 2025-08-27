package app.notesr.exporter.service;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
abstract class Exporter {
    private final Runnable notifyProgress;

    @Getter
    private long exported = 0;

    protected void increaseProgress() {
        exported++;
        notifyProgress.run();
    }

    public abstract void export() throws IOException, InterruptedException;
    public abstract long getTotal();
}
