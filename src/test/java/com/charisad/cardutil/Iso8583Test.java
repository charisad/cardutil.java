package com.charisad.cardutil;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Iso8583Test {

    @Test
    void testPackUnpack() {
        // Example from Python docstring
        // message_dict = {'MTI': '1144', 'DE2': '4444555566667777'}
        Map<String, Object> data = new HashMap<>();
        data.put("MTI", "1144");
        data.put("DE2", "4444555566667777");

        byte[] packed = Iso8583.pack(data, null);
        
        // Unpack
        Map<String, Object> unpacked = Iso8583.unpack(packed, null);
        
        assertEquals("1144", unpacked.get("MTI"));
        assertEquals("4444555566667777", unpacked.get("DE2"));
    }

    @Test
    void testHexBitmap() {
        Map<String, Object> data = new HashMap<>();
        data.put("MTI", "1144");
        data.put("DE2", "4444555566667777");

        byte[] packed = Iso8583.pack(data, null, StandardCharsets.ISO_8859_1, true);
        String packedStr = new String(packed);
        // Expect MTI (4) + Bitmap (32 hex) + Data
        assertEquals("1144", packedStr.substring(0, 4));
        assertEquals(32, packedStr.substring(4, 36).length());
        
        Map<String, Object> unpacked = Iso8583.unpack(packed, null, StandardCharsets.ISO_8859_1, true);
        assertEquals("1144", unpacked.get("MTI"));
        assertEquals("4444555566667777", unpacked.get("DE2"));
    }
}
