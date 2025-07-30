package app.notesr.service.data.importer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ImportStatus {
    DECRYPTING("decrypting"),
    IMPORTING("running"),
    CLEANING_UP("cleaning_up"),
    DONE("decryption_failed"),
    DECRYPTION_FAILED("decryption_failed"),
    IMPORT_FAILED("import_failed");

    private final String status;
}
