package app.notesr.manager.importer;

public enum ImportResult {
    NONE("none"),
    FINISHED_SUCCESSFULLY("finished_successfully"),
    DECRYPTION_FAILED("decryption_failed"),
    IMPORT_FAILED("import_failed");

    public final String result;

    ImportResult(String result) {
        this.result = result;
    }
}
