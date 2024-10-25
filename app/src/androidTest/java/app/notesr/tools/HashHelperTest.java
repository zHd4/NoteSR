package app.notesr.tools;

import org.junit.Assert;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HashHelperTest {

    private static final int MAX_DATA_SIZE = 100000;
    private final Random random = new Random();

    @Test
    public void testSha256Bytes() throws NoSuchAlgorithmException {
        byte[] data = new byte[random.nextInt(MAX_DATA_SIZE)];
        random.nextBytes(data);

        byte[] expected = MessageDigest.getInstance("SHA-256").digest(data);
        byte[] actual = HashHelper.toSha256Bytes(data);

        Assert.assertArrayEquals("Actual hash different", expected, actual);
    }
}
