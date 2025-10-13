package app.notesr.core.security.crypto;

public interface CryptorFactory {
    AesCryptor create(String password, Class<? extends AesCryptor> cryptorClass) throws Exception;
}
