package com.notesr;

import com.peew.notesr.crypto.CryptoTools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SetupKeyTest {
    private static final byte[] keyBytes = new byte[] {
            28, -65, -106, -25,
            -88, -127, 7, -32,
            -47, 28, 95, -85,
            89, -51, 37, 97,
            -27, -5, -125, 68,
            104, 39, -9, -22,
            -79, -82, 4, -64,
            -121, -59, -29, -62 };

    private static final String keyHex = """
            1C BF 96 E7\s
            A8 81 07 E0\s
            D1 1C 5F AB\s
            59 CD 25 61\s
            E5 FB 83 44\s
            68 27 F7 EA\s
            B1 AE 04 C0\s
            87 C5 E3 C2""";

    @Test
    public void testKeyConvertation() {
        String actual = CryptoTools.keyBytesToHex(keyBytes);
        Assertions.assertEquals(keyHex, actual);

        byte[] actualBytes = CryptoTools.hexKeyToBytes(keyHex);
        Assertions.assertArrayEquals(keyBytes, actualBytes);
    }
}
