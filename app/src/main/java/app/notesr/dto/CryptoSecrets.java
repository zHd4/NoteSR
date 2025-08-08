package app.notesr.dto;

import java.io.Serializable;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CryptoSecrets implements Serializable {

    private byte[] key;
    private String password;

    public static CryptoSecrets from(CryptoSecrets cryptoKey) {
        return new CryptoSecrets(Arrays.copyOf(cryptoKey.key, cryptoKey.key.length),
                cryptoKey.password);
    }
}
