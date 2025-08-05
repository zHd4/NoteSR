package app.notesr.service.crypto;

import java.io.IOException;

import app.notesr.crypto.CryptoManager;
import app.notesr.dto.CryptoSecrets;
import app.notesr.exception.EncryptionFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SecretsSetupService {
    private final CryptoManager cryptoManager;
    private final CryptoSecrets cryptoSecrets;

    public SecretsSetupService(CryptoManager cryptoManager, String password) {
        this.cryptoManager = cryptoManager;
        this.cryptoSecrets = cryptoManager.generateSecrets(password);
    }

    public void apply() throws EncryptionFailedException, IOException {
        cryptoManager.setSecrets(cryptoSecrets);
    }
}
