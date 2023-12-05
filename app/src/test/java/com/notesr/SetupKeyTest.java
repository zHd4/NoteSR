package com.notesr;

import com.peew.notesr.crypto.CryptoTools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SetupKeyTest {
    private static final byte[] keyBytes = new byte[] {
            -38, -111, 120, -87,
            -28, -119, 126, 121,
            43, -89, -112, -99,
            -31, -30, 109, 53,
            -101, -102, -82, 41,
            21, -53, -96, -95,
            -16, 104, -90, -122,
            45, -13, -53, -115 };

    private static final String keyHex =
            "da 91 78 a9 \n" +
            "e4 89 7e 79 \n" +
            "2b a7 90 9d \n" +
            "e1 e2 6d 35 \n" +
            "9b 9a ae 29 \n" +
            "15 cb a0 a1 \n" +
            "f0 68 a6 86 \n" +
            "2d f3 cb 8d";

    @Test
    public void testKeyConvertation() {
        String actual = CryptoTools.keyBytesToHex(keyBytes);
        Assertions.assertEquals(keyHex, actual);

        byte[] actualBytes = CryptoTools.hexKeyToBytes(keyHex);
        Assertions.assertArrayEquals(keyBytes, actualBytes);
    }
}
