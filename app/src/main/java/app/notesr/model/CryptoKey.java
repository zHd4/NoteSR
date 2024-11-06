package app.notesr.model;

import androidx.annotation.NonNull;
import javax.crypto.SecretKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class CryptoKey {

    private SecretKey key;
    private byte[] salt;
    private String password;

    @NonNull
    @Override
    public CryptoKey clone() {
        return new CryptoKey(key, salt, password);
    }
}
