package com.charisad.cardutil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class PinBlock {

    /**
     * Calculate ISO-0 (Format 0) PIN Block.
     * Also known as ANSI X9.8, Visa-1, ECI-0.
     *
     * @param pin The PIN (e.g. "1234")
     * @param pan The PAN (Card Number)
     * @return 8-byte PIN Block
     */
    public static byte[] calculateIso0(String pin, String pan) {
        // P1 = 0 L P P P P F/P F F F F F F F F F F
        // L = PIN length
        // P = PIN digit
        // F = Padding (0xF)
        
        StringBuilder sb1 = new StringBuilder();
        sb1.append("0");
        sb1.append(pin.length());
        sb1.append(pin);
        while (sb1.length() < 16) {
            sb1.append("F");
        }
        byte[] p1 = hexStringToByteArray(sb1.toString());

        // P2 = 0 0 0 0 C C C C C C C C C C C C
        // C = Rightmost 12 digits of PAN excluding check digit
        // PAN: ...1234567890123L (L is check digit)
        // We take 12 digits before L.
        if (pan.length() < 13) {
            throw new IllegalArgumentException("PAN must be at least 13 digits");
        }
        String panPart = pan.substring(pan.length() - 13, pan.length() - 1);
        String p2Str = "0000" + panPart;
        byte[] p2 = hexStringToByteArray(p2Str);

        byte[] pinBlock = new byte[8];
        for (int i = 0; i < 8; i++) {
            pinBlock[i] = (byte) (p1[i] ^ p2[i]);
        }
        return pinBlock;
    }

    /**
     * Calculate ISO-4 (Format 4) PIN Block.
     * Starts with 4, includes PIN, padding A, and random values.
     *
     * @param pin The PIN
     * @return 16-byte PIN Block
     */
    public static byte[] calculateIso4(String pin) {
        // 4 L P P P P a a a a a a A A R R R R R R ...
        // 4 = Format
        // L = PIN Length
        // P = PIN
        // a = PIN extension? (Assume just PIN here)
        // A = Padding (0xA)
        // R = Random
        
        // Python: f'{"4" + str(len(self.pin)) + self.pin:a<16}{self.random_value:016x}'
        // The python code pads with 'a' (lowercase?) up to 16 chars?
        // Wait, Python: `"4...":a<16` means align left, pad with 'a' to 16 chars.
        // And then random value 16 chars (8 bytes hex).
        // Format 4 is AES, so 16 bytes block.
        // It seems Python implementation: 
        // Part 1: "4" + len + pin. Pad with "a" to 16 chars (hex nibbles -> 8 bytes).
        // Part 2: Random 8 bytes (16 hex chars).
        // Total 32 hex chars -> 16 bytes.
        
        StringBuilder sb = new StringBuilder();
        sb.append("4");
        sb.append(String.format("%X", pin.length())); // L is hex digit? usually pin len < 12.
        sb.append(pin);
        
        while (sb.length() < 16) {
            sb.append("A"); // Python uses 'a', hex A.
        }
        
        // Random 8 bytes
        byte[] random = new byte[8];
        new SecureRandom().nextBytes(random);
        String rndHex = binAsciiHexlify(random);
        sb.append(rndHex);
        
        return hexStringToByteArray(sb.toString());
    }

    /**
     * Encrypt PIN Block using TDES (ECB, NoPadding).
     * @param pinBlock 8-byte PIN Block
     * @param key Hex string of key (16 bytes = 32 hex chars for Double key, or 24 bytes for Triple)
     * @return Encrypted bytes
     */
    public static byte[] encryptTdes(byte[] pinBlock, String key) {
        try {
            byte[] keyBytes = hexStringToByteArray(key);
            // Java TDES key must be 24 bytes. If 16 bytes provided (double length), we need to duplicate first 8 bytes?
            // "TripleDES" in Java usually expects 24 bytes.
            // But if we pass "DESede", it handles 16 or 24.
            // Let's check Python `d_algorithms.TripleDES(binary_key)`.
            // If key is 16 bytes, Python cryptography handles it.
            // Java `SecretKeySpec(key, "DESede")` works with 24 bytes.
            // If 16 bytes, we might need to adjust.
            if (keyBytes.length == 16) {
                byte[] k24 = new byte[24];
                System.arraycopy(keyBytes, 0, k24, 0, 16);
                System.arraycopy(keyBytes, 0, k24, 16, 8); // K1 = K3
                keyBytes = k24;
            }
            
            SecretKey skey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            return cipher.doFinal(pinBlock);
        } catch (Exception e) {
            throw new RuntimeException("TDES Encryption failed", e);
        }
    }

    /**
     * Calculate Visa PVV.
     */
    public static String calculatePvv(String pin, String pvvKey, int keyIndex, String pan) {
        try {
            // TSP = Rightmost 11 of PAN (excluding check) + Key Index + PIN
            if (pan.length() < 12) throw new IllegalArgumentException("PAN too short");
            String rightmost11 = pan.substring(pan.length() - 12, pan.length() - 1);
            String tsp = rightmost11 + keyIndex + pin;
            
            byte[] keyBytes = hexStringToByteArray(pvvKey);
            // PVV Key usually 16 bytes (Double length DES)?
            if (keyBytes.length == 16) {
                 byte[] k24 = new byte[24];
                 System.arraycopy(keyBytes, 0, k24, 0, 16);
                 System.arraycopy(keyBytes, 0, k24, 16, 8);
                 keyBytes = k24;
            }
            
            SecretKey skey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            
            // TSP is numeric string. Python: "encryptor.update(binascii.unhexlify(tsp))"
            // Wait, Python `binascii.unhexlify(tsp)` means TSP is Hex String?
            // "tsp = ... + pin". PIN is digits. PAN is digits.
            // So TSP is a string of digits.
            // In ISO/PVV, TSP is treated as Hex?
            // E.g. "12345678901" + "1" + "1234" = "1234567890111234" (16 chars).
            // Converted to 8 bytes.
            byte[] tspBytes = hexStringToByteArray(tsp);
            
            byte[] ct = cipher.doFinal(tspBytes);
            String ctHex = binAsciiHexlify(ct);
            
            StringBuilder pvv = new StringBuilder();
            // Pass 1: digits 0-9
            for (char c : ctHex.toCharArray()) {
                if (Character.isDigit(c)) pvv.append(c);
                if (pvv.length() == 4) break;
            }
            // Pass 2: A-F -> 0-5
            if (pvv.length() < 4) {
               for (char c : ctHex.toCharArray()) {
                   if (!Character.isDigit(c)) {
                       int v = Character.digit(c, 16); // A=10
                       pvv.append(v - 10);
                   }
                   if (pvv.length() == 4) break;
               }
            }
            return pvv.toString();

        } catch (Exception e) {
            throw new RuntimeException("PVV Calculation failed", e);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    private static String binAsciiHexlify(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
