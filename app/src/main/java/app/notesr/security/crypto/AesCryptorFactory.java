package app.notesr.security.crypto;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;

public final class AesCryptorFactory implements CryptorFactory {
    @Override
    public AesCryptor create(String password, Class<? extends AesCryptor> cryptorClass)
            throws NoSuchAlgorithmException {
        byte[] salt = AesCryptor.generatePasswordBasedSalt(password);

        try {
            return cryptorClass.getConstructor(String.class, byte[].class)
                    .newInstance(password, salt);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
