package app.notesr.security.service;

import app.notesr.security.crypto.CryptoManager;
import app.notesr.db.AppDatabase;
import app.notesr.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretsUpdateService {
    private final AppDatabase db;
    private final CryptoManager cryptoManager;
    private final CryptoSecrets newSecrets;

    public void update() {
        db.runInTransaction(() -> {
            String keyHex = toHex(newSecrets.getKey());

            db.getOpenHelper().getWritableDatabase()
                    .execSQL("PRAGMA rekey = x'" + keyHex + "'");
            cryptoManager.setSecrets(newSecrets);
            return null;
        });
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
