package app.notesr.exporter.service;

import java.io.IOException;

interface Exporter {
    void export() throws IOException, InterruptedException;
    long getTotal();
}
