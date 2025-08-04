package app.notesr.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;

import javax.crypto.SecretKey;

public class AesCryptorBaseTest {
    @Test
    public void testGenerateRandomKeyShouldBeValid() throws Exception {
        SecretKey key = AesCryptor.generateRandomKey();

        assertNotNull(key);
        assertEquals(32, key.getEncoded().length);
    }

    @Test
    public void testGeneratePasswordSaltShouldBe16Bytes() throws Exception {
        byte[] salt = AesCryptor.generatePasswordBasedSalt("password");
        assertEquals(16, salt.length);
    }
}
