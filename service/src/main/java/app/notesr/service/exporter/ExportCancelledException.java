package app.notesr.service.exporter;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ExportCancelledException extends RuntimeException {

    public ExportCancelledException(Throwable cause) {
        super(cause);
    }
}
