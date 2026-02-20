package com.charisad.cardutil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

public class Key {

    /**
     * Combine key components (XOR).
     * @return hex string of combined key.
     */
    public static String getZoneMasterKey(String... keyParts) {
        BigInteger combined = BigInteger.ZERO;
        // Python: p1 = '00' * 16 (16 bytes = 128 bits)
        // int(p1, 16) ^ int(keyPart, 16)
        
        for (String part : keyParts) {
            BigInteger partInt = new BigInteger(part, 16);
            combined = combined.xor(partInt);
        }
        
        // Pad to 32 hex chars (16 bytes) or length of input?
        // Python code initializes p1 = '00'*16.
        // If key parts are 32 chars (16 bytes), result is 16 bytes.
        String hex = combined.toString(16).toUpperCase();
        while (hex.length() < 32) {
             hex = "0" + hex;
        }
        return hex;
    }
    
    public static String calculateKcv(String keyHex) {
       return calculateKcv(keyHex, 6);
    }

    public static String calculateKcv(String keyHex, int length) {
        try {
            byte[] keyBytes = hexStringToByteArray(keyHex);
            if (keyBytes.length == 16) {
                 byte[] k24 = new byte[24];
                 System.arraycopy(keyBytes, 0, k24, 0, 16);
                 System.arraycopy(keyBytes, 0, k24, 16, 8);
                 keyBytes = k24;
            }
            
            SecretKey skey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            
            // Encrypt 16 bytes of 00. Python: b'\x00' * 16
            // Wait, Python default_backend TDES block size is 8 bytes.
            // But update(b'\x00' * 16) -> encrypts 2 blocks?
            // "ct = encryptor.update(b'\x00' * 16) + encryptor.finalize()"
            // KCV is usually first 3 bytes (6 hex) of result.
            // If we encrypt 16 bytes, we get 16 bytes output.
            // KCV take first `length` hex chars.
            
            byte[] zeros = new byte[16]; // 16 bytes of zeros?
            // Usually KCV is 8 bytes block.
            // Python code sends 16 bytes of zeros.
            byte[] ct = cipher.doFinal(zeros);
            
            String ctHex = binAsciiHexlify(ct);
            if (ctHex.length() > length) {
                return ctHex.substring(0, length);
            }
            return ctHex;
        } catch (Exception e) {
            throw new RuntimeException("KCV Calculation failed", e);
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
