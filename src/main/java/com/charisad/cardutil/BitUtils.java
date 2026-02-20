package com.charisad.cardutil;

import java.util.BitSet;

public class BitUtils {

    /**
     * Converts a byte array to a BitSet.
     * The first bit of the byte array (byte 0, bit 7) becomes bit 0 of the BitSet.
     * Note: BitSet grows automatically.
     */
    public static BitSet fromBytes(byte[] bytes) {
        BitSet bits = new BitSet(bytes.length * 8);
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    /**
     * Converts a BitSet to a byte array.
     * @param bits The BitSet to convert.
     * @param lengthBytes The expected length of the byte array (e.g., 8 bytes for 64 bits, 16 bytes for 128 bits).
     *                    If the BitSet has fewer bits, the result is padded with zeros.
     */
    public static byte[] toBytes(BitSet bits, int lengthBytes) {
        byte[] bytes = new byte[lengthBytes];
        for (int i = 0; i < lengthBytes * 8; i++) {
            if (bits.get(i)) {
                bytes[i / 8] |= (1 << (7 - (i % 8)));
            }
        }
        return bytes;
    }
}
