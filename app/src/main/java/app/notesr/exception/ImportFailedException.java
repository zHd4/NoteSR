package app.notesr.exception;

public class ImportFailedException extends Exception {

    public ImportFailedException(String message) {
        super(message);
    }

    public ImportFailedException(Exception e) {
        super(e);
    }
}
