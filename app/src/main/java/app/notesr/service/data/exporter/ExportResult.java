package app.notesr.service.data.exporter;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExportResult {
    NONE("none"),
    FINISHED_SUCCESSFULLY("finished_successfully"),
    CANCELED("canceled");

    private final String result;
}
