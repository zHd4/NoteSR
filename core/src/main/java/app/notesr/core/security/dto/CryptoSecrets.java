package app.notesr.core.security.dto;

import java.io.Serializable;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public final class CryptoSecrets implements Serializable {

    private byte[] key;
    private String password;

    public static CryptoSecrets from(CryptoSecrets secrets) {
        return new CryptoSecrets(Arrays.copyOf(secrets.key, secrets.key.length),
                secrets.password);
    }
}
