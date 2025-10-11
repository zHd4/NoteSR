package app.notesr.cleaner.service;

import java.util.List;

import app.notesr.cleaner.model.TempFile;
import app.notesr.db.AppDatabase;
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
