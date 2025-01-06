package app.notesr.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

import app.notesr.TestBase;

public class HashHelperTest extends TestBase {

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

    @Test
    public void testGetUUIDHash() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        long expected = 3121068470L;
        long actual = HashHelper.getUUIDHash(uuid);

        Assert.assertEquals("Actual hash different", expected, actual);
    }
}
