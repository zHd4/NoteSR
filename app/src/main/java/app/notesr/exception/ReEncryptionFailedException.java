package app.notesr.exception;

public class ReEncryptionFailedException extends RuntimeException {
    public ReEncryptionFailedException(Exception e) {
        super(e);
    }
}
