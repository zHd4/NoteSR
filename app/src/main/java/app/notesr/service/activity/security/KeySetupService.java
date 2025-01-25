package app.notesr.service.activity.security;

import static app.notesr.crypto.CryptoTools.cryptoKeyToHex;
import static app.notesr.crypto.CryptoTools.hexToCryptoKey;

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

@Getter
public class KeySetupService {
    private CryptoKey cryptoKey;

    public KeySetupService(String password) {
        try {
            this.cryptoKey = getCryptoManager().generateNewKey(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHexKey() {
        return cryptoKeyToHex(cryptoKey);
    }

    public void setHexKey(String hexKey) throws Exception {
        String password = cryptoKey.getPassword();
        this.cryptoKey = hexToCryptoKey(hexKey, password, true);
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
