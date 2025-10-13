package app.notesr.security.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DecryptionFailedException extends Exception {
    public DecryptionFailedException(Throwable cause) {
        super(cause);
    }
}
