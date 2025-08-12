package app.notesr.security.service;

import android.content.Context;

import java.io.IOException;

import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.exception.EncryptionFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SecretsSetupService {
    private final Context context;
    private final CryptoManager cryptoManager;
    private final CryptoSecrets cryptoSecrets;

    public SecretsSetupService(Context context, CryptoManager cryptoManager, String password) {
        this.context = context;
        this.cryptoManager = cryptoManager;
        this.cryptoSecrets = cryptoManager.generateSecrets(password);
    }

    public void apply() throws EncryptionFailedException, IOException {
        cryptoManager.setSecrets(context, cryptoSecrets);
    }
}
