package app.notesr.dto;

import java.io.Serializable;

import javax.crypto.SecretKey;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CryptoKey implements Serializable {

    private SecretKey key;
    private byte[] salt;
    private String password;

    public static CryptoKey from(CryptoKey cryptoKey) {
        return new CryptoKey(cryptoKey.key, cryptoKey.salt, cryptoKey.password);
    }
}
