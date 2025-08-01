package app.notesr.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import app.notesr.crypto.AesCryptor;
import app.notesr.dto.CryptoKey;
import io.bloco.faker.Faker;

import org.junit.BeforeClass;
import org.junit.Test;

import java.security.InvalidKeyException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtilsTest {

    private static final Faker FAKER = new Faker();

    private static final byte[] TEST_KEY_BYTES = new byte[] {
            28, -65, -106, -25,
            -88, -127, 7, -32,
            -47, 28, 95, -85,
            89, -51, 37, 97,
            -27, -5, -125, 68,
            104, 39, -9, -22,
            -79, -82, 4, -64,
            -121, -59, -29, -62 };

    private static final byte[] TEST_SALT = new byte[] {
            -4, -106, -91, -41,
            -13, 24, 77, 85,
            -100, -16, -116, -24,
            -8, -48, 95, 85 };

    private static final String TEST_HEX_CRYPTO_KEY = """
            1C BF 96 E7\s
            A8 81 07 E0\s
            D1 1C 5F AB\s
            59 CD 25 61\s
            E5 FB 83 44\s
            68 27 F7 EA\s
            B1 AE 04 C0\s
            87 C5 E3 C2\s
            FC 96 A5 D7\s
            F3 18 4D 55\s
            9C F0 8C E8\s
            F8 D0 5F 55""";

    private static String testPassword;

    @BeforeClass
    public static void beforeAll() {
        testPassword = FAKER.internet.password();
    }

    @Test
    public void testCryptoKeyConvertation() throws InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(TEST_KEY_BYTES, 0, TEST_KEY_BYTES.length,
                AesCryptor.KEY_GENERATOR_ALGORITHM);
        CryptoKey cryptoKey = new CryptoKey(secretKey, TEST_SALT, testPassword);

        String actual = CryptoUtils.cryptoKeyToHex(cryptoKey);
        assertThat(actual, is(TEST_HEX_CRYPTO_KEY));

        CryptoKey actualCryptoKey = CryptoUtils.hexToCryptoKey(TEST_HEX_CRYPTO_KEY, testPassword);

        assertThat(actualCryptoKey, is(cryptoKey));
    }
}
