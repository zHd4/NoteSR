package app.notesr.service.migration;

public class AppMigrationException extends RuntimeException {
    public AppMigrationException(String message) {
        super(message);
    }

    public AppMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
