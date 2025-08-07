package app.notesr.service.file;

import java.util.List;

import app.notesr.db.AppDatabase;
import app.notesr.model.TempFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TempFileService {
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
