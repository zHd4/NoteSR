package app.notesr.service.data.exporter;

public class ExportFailedException extends RuntimeException {

    public ExportFailedException(String message) {
        super(message);
    }
}
