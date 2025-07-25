package app.notesr.service.crypto;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import app.notesr.App;
import app.notesr.crypto.CryptoManager;
import app.notesr.dto.CryptoKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KeySetupService {
    private final CryptoKey cryptoKey;

    public KeySetupService(String password) throws NoSuchAlgorithmException {
        this.cryptoKey = getCryptoManager().generateNewKey(password);
    }

    public void apply() throws InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException,
            BadPaddingException, IOException, InvalidKeyException {
        getCryptoManager().applyNewKey(cryptoKey);
    }

    private CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
