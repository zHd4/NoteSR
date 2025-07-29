package app.notesr.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CryptoSecrets implements Serializable {

    private byte[] key;
    private String password;

    public static CryptoSecrets from(CryptoSecrets cryptoKey) {
        return new CryptoSecrets(cryptoKey.key, cryptoKey.password);
    }
}
