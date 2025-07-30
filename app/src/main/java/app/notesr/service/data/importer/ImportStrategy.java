package app.notesr.service.data.importer;

public interface ImportStrategy {
    void doImport();
    ImportStatus getStatus();
}
