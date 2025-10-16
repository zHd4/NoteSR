package app.notesr.core.security.crypto;

public interface CryptorFactory {
    AesCryptor create(char[] password, Class<? extends AesCryptor> cryptorClass) throws Exception;
}
