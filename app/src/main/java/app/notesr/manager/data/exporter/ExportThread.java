package app.notesr.manager.data.exporter;

import android.util.Log;
import java.io.IOException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ExportThread extends Thread {
    private static final String TAG = ExportThread.class.getName();

    private final ExportManager manager;
    private boolean running = false;

    @Override
    public void start() {
        running = true;
        super.start();
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        boolean interrupted = super.isInterrupted();
        return !running || interrupted;
    }

    public void breakOnInterrupted() throws InterruptedException {
        if (isInterrupted()) {
            throw new InterruptedException(TAG + " interrupted");
        }
    }

    @Override
    public void run() {
        try {
            manager.doExport();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            interrupt();
            manager.onThreadInterrupted();
        }
    }
}
