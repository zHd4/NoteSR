package app.notesr.dto;

import androidx.annotation.NonNull;
import javax.crypto.SecretKey;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CryptoKey implements Cloneable {

    private SecretKey key;
    private byte[] salt;
    private String password;

    @NonNull
    @Override
    public CryptoKey clone() throws CloneNotSupportedException {
        CryptoKey cryptoKey = (CryptoKey) super.clone();

        cryptoKey.key = this.key;
        cryptoKey.salt = this.salt;
        cryptoKey.password = this.password;

        return cryptoKey;
    }
}
