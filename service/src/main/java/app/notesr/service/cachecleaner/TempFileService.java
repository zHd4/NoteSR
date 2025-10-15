package app.notesr.service.cachecleaner;

import java.util.List;

import app.notesr.data.model.TempFile;
import app.notesr.data.AppDatabase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TempFileService {
    private final AppDatabase db;

    public void save(TempFile file) {
        db.getTempFileDao().insert(file);
    }

    public List<TempFile> getAll() {
        return db.getTempFileDao().getAll();
    }

    public void delete(Long id) {
        db.getTempFileDao().deleteById(id);
    }
}
