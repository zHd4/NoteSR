package app.notesr.service.data.exporter;

import java.io.IOException;

interface Exporter {
    void export() throws IOException, InterruptedException;
    long getTotal();
}
