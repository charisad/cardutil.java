package com.charisad.cardutil;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinBlockTest {

    @Test
    void testIso0() {
        // Example from Python docstring
        // pin='1234', card='1111222233334444' -> 041226dddccccbbb
        byte[] block = PinBlock.calculateIso0("1234", "1111222233334444");
        String hex = binAsciiHexlify(block);
        assertEquals("041226DDDCCCCBBB", hex);
    }

    private static String binAsciiHexlify(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
