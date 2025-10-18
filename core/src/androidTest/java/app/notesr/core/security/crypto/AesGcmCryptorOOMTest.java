package app.notesr.core.security.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AesGcmCryptorOOMTest {
    private static final Random RANDOM = new Random();
    private static final String PASSWORD = "gcmPassword";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AesGcmCryptor cryptor;

    @Before
    public void setUp() throws Exception {
        byte[] salt = AesGcmCryptor.generatePasswordBasedSalt(PASSWORD.toCharArray());
        cryptor = new AesGcmCryptor(PASSWORD.toCharArray(), salt);
    }

    @Test
    public void testLargeStreamEncryptionAndDecryption() throws Exception {
        File largeFile = tempFolder.newFile("large_file");

        try (FileOutputStream out = new FileOutputStream(largeFile)) {
            for (int i = 0; i < 200; i++) {
                byte[] data = new byte[1_000_000];
                RANDOM.nextBytes(data);

                out.write(data);
            }
        }

        File encryptedFile = tempFolder.newFile("encrypted_file");

        FileInputStream plainIn = new FileInputStream(largeFile);
        FileOutputStream encryptedOut = new FileOutputStream(encryptedFile);

        cryptor.encrypt(plainIn, encryptedOut);

        File decryptedFile = tempFolder.newFile("decrypted_file");

        FileInputStream encryptedIn = new FileInputStream(encryptedFile);
        FileOutputStream decryptedOut = new FileOutputStream(decryptedFile);

        cryptor.decrypt(encryptedIn, decryptedOut);

        String originalSha512 = computeSha512(largeFile);
        String decryptedSha512 = computeSha512(decryptedFile);

        assertEquals("Decrypted file must have the same SHA-512 hash as the original",
                originalSha512, decryptedSha512);
    }

    private static String computeSha512(File file)
            throws NoSuchAlgorithmException, IOException {

        MessageDigest digest = MessageDigest.getInstance("SHA-512");

        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder hexDigestBuilder = new StringBuilder();

        for (byte b : digest.digest()) {
            hexDigestBuilder.append(String.format("%02x", b));
        }

        return hexDigestBuilder.toString();
    }
}