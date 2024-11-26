package app.notesr.manager.data.importer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ImportResult {
    NONE("none"),
    FINISHED_SUCCESSFULLY("finished_successfully"),
    DECRYPTION_FAILED("decryption_failed"),
    IMPORT_FAILED("import_failed");

    public final String result;
}
