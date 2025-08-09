package app.notesr.exporter.service;

public class ExportFailedException extends RuntimeException {

    public ExportFailedException(String message) {
        super(message);
    }
}
