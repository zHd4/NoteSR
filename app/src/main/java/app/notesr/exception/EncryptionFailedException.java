package app.notesr.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EncryptionFailedException extends Exception {
    public EncryptionFailedException(Throwable cause) {
        super(cause);
    }
}
