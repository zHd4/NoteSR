package app.notesr.importer.exception;

public class ImportFailedException extends RuntimeException {

    public ImportFailedException(String message) {
        super(message);
    }

    public ImportFailedException(Exception e) {
        super(e);
    }
}
