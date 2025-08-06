package app.notesr.service.data.importer;

public interface ImportStrategy {
    void execute();
    ImportStatus getStatus();
}
