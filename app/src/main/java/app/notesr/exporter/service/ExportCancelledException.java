package app.notesr.exporter.service;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ExportCancelledException extends RuntimeException {

    public ExportCancelledException(Throwable cause) {
        super(cause);
    }
}
