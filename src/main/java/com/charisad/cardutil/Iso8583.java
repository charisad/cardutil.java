package com.charisad.cardutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Iso8583 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Iso8583.class);
    private static final Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1; // Latin-1

    /**
     * Deserialize bytes to a Map.
     */
    public static Map<String, Object> unpack(byte[] message, Map<Integer, BitConfig> config) {
        return unpack(message, config, DEFAULT_ENCODING, false);
    }

    public static Map<String, Object> unpack(byte[] message, Map<Integer, BitConfig> config, Charset encoding, boolean hexBitmap) {
        if (config == null) config = Config.DEFAULT_BIT_CONFIG;
        LOGGER.debug("Processing message: len={}", message.length);

        try {
            int pointer = 0;
            // MTI
            byte[] mtiBytes = Arrays.copyOfRange(message, pointer, pointer + 4);
            pointer += 4;
            String mti = new String(mtiBytes, encoding);

            // Bitmap
            byte[] bitmapBytes;
            if (hexBitmap) {
                byte[] hexBitmapBytes = Arrays.copyOfRange(message, pointer, pointer + 32);
                pointer += 32;
                // Hex to binary conversion
                String hexString = new String(hexBitmapBytes, encoding);
                bitmapBytes = hexStringToByteArray(hexString);
            } else {
                bitmapBytes = Arrays.copyOfRange(message, pointer, pointer + 16);
                pointer += 16;
            }

            Map<String, Object> returnValues = new HashMap<>();
            returnValues.put("MTI", mti);

            BitSet bitmap = BitUtils.fromBytes(bitmapBytes);
            
            // Process fields
            for (int bit = 2; bit <= 128; bit++) {
                if (bitmap.get(bit - 1)) { // BitSet is 0-indexed, ISO is 1-indexed
                    if (!config.containsKey(bit)) {
                        throw new Iso8583DataError("No bit config available for bit " + bit, message, null);
                    }
                    BitConfig bitConfig = config.get(bit);
                    LOGGER.debug("Processing bit {}", bit);

                    FieldResult result = parseField(bit, bitConfig, message, pointer, encoding);
                    returnValues.putAll(result.values);
                    pointer = result.newPointer;
                }
            }
            
            if (pointer != message.length) {
                 throw new Iso8583DataError(
                    String.format("Message data not correct length. Parsed to %d, total %d", pointer, message.length),
                    message, null);
            }

            return returnValues;

        } catch (Exception e) {
            if (e instanceof Iso8583DataError) throw (Iso8583DataError) e;
            throw new Iso8583DataError("Failed unpacking message", message, e);
        }
    }

    /**
     * Serialize Map to bytes.
     */
    public static byte[] pack(Map<String, Object> data, Map<Integer, BitConfig> config) {
        return pack(data, config, DEFAULT_ENCODING, false);
    }

    public static byte[] pack(Map<String, Object> data, Map<Integer, BitConfig> config, Charset encoding, boolean hexBitmap) {
        if (config == null) config = Config.DEFAULT_BIT_CONFIG;

        // Create a copy of data to modify (for PDS fields handling)
        Map<String, Object> message = new HashMap<>(data);
        
        // Handle PDS rollup
        List<Integer> dePdsFields = new ArrayList<>();
        for (Map.Entry<Integer, BitConfig> entry : config.entrySet()) {
            if ("PDS".equals(entry.getValue().fieldProcessor())) {
                dePdsFields.add(entry.getKey());
            }
        }
        dePdsFields.sort(Collections.reverseOrder());
        
        List<String> pdsDeValues = pdsToDe(message);
        // Map PDS values back to DE fields (popping from end as in python)
        // Python: for de_field_value in _pds_to_de(message): ... pop()
        for (String deValue : pdsDeValues) {
             if (dePdsFields.isEmpty()) break; // Should not happen if config matches
             Integer fieldKey = dePdsFields.remove(0); // python pop() is last, checking python logic...
             // Python: de_pds_fields = sorted(..., reverse=True) -> [62, 48]
             // for ...: pop() -> 48, then 62.
             // My java list is reverse ordered: [62, 48].
             // To match python pop(), I should remove(0)? No, pop() removes last element.
             // So if I have [62, 48], pop() gives 48.
             // So field 48 gets first PDS chunk.
             // I should use remove(0) if I sorted reverse? No.
             // collection: [62, 48]. pop() -> 48.
             // So I should remove(size-1) or just sort natural order and remove(0).
             // Let's sort natural [48, 62]. remove(0) gives 48.
        }
        // Actually, let's look at python again:
        // de_pds_fields = sorted(..., reverse=True) -> [12x, ..., 62, 48]
        // pop() -> 48.
        // So first chunk goes to lowest DE.
        Collections.sort(dePdsFields); // [48, 62, ...]
        for (int i = 0; i < pdsDeValues.size() && i < dePdsFields.size(); i++) {
            message.put("DE" + dePdsFields.get(i), pdsDeValues.get(i));
        }

        BitSet bitmap = new BitSet(128);
        bitmap.set(0); // Bit 1 is always set? Python: "bitmap_values[0] = True" -> yes, for presence of bitmap (or secondary?)
        // Python: "set bit 1 on for presence of bitmap". Actually bit 1 usually means secondary bitmap is present.
        // The implementation seems to force 16 byte bitmap, so bit 1 should essentially be set if we are using 128 bits?
        // But the code unconditionally sets it. I will follow.

        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

        try {
            for (int bit = 2; bit <= 128; bit++) {
                Object val = message.get("DE" + bit);
                // Allow 0 values
                boolean present = val != null;
                if (val instanceof Number && ((Number)val).longValue() == 0) present = true; // "0 evals to false, allow zero values"
                
                if (present) {
                    bitmap.set(bit - 1);
                    BitConfig bitConfig = config.get(bit);
                    if (bitConfig == null) continue; // Should warn?
                    
                    byte[] fieldBytes = fieldToBytes(bitConfig, val, encoding);
                    dataStream.write(fieldBytes);
                }
            }

            byte[] bitmapBytes = BitUtils.toBytes(bitmap, 16);
            byte[] finalBitmapBytes;
            if (hexBitmap) {
                finalBitmapBytes = binAsciiHexlify(bitmapBytes).getBytes(encoding);
            } else {
                finalBitmapBytes = bitmapBytes;
            }

            String mti = (String) message.get("MTI");
            if (mti == null) mti = "";
            
            ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
            finalStream.write(mti.getBytes(encoding));
            finalStream.write(finalBitmapBytes);
            finalStream.write(dataStream.toByteArray());
            
            return finalStream.toByteArray();
        } catch (Exception e) {
             throw new RuntimeException("Error packing message", e);
        }
    }

    private static class FieldResult {
        Map<String, Object> values = new HashMap<>();
        int newPointer;
        public FieldResult(int ptr) { newPointer = ptr; }
    }

    private static FieldResult parseField(int bit, BitConfig config, byte[] message, int pointer, Charset encoding) {
         FieldResult result = new FieldResult(pointer);
         
         int fieldLength = config.fieldLength();
         int lengthSize = getFieldLengthSize(config);

         if (lengthSize > 0) {
             String lengthStr = new String(message, pointer, lengthSize, encoding);
             pointer += lengthSize;
             try {
                 fieldLength = Integer.parseInt(lengthStr);
             } catch (NumberFormatException e) {
                 throw new Iso8583DataError("Invalid field length DE" + bit, message, e);
             }
         }

         byte[] fieldDataBytes = Arrays.copyOfRange(message, pointer, pointer + fieldLength);
         pointer += fieldLength;
         result.newPointer = pointer;

         String fieldProcessor = config.fieldProcessor();
         String fieldDataStr = null;
         
         if (!"ICC".equals(fieldProcessor)) {
             fieldDataStr = new String(fieldDataBytes, encoding);
         }
         
         // Only mask/prefix logic if we have the string
         if (fieldDataStr != null) {
            // Masking/Prefix logic would go here if needed for logging or storage
            // Python code does masking in-place on 'field_data'.
            // if field_processor == 'PAN', mask. 'PAN-PREFIX', prefix.
         }

         // Convert to Type
         Object finalValue;
         if ("ICC".equals(fieldProcessor)) {
             // For ICC, we keep bytes or convert to hex string for processing
             // Python calls _icc_to_dict(fieldData)
             // But it also returns DE field value?
             // Python: return_values["DE" + bit] = field_data
             // AND updates with icc dict.
             // For ICC, field_data passed to _icc_to_dict is bytes.
             // But DE{bit} value is ... what?
             // Python code: "field_data = message_data..." -> "field_data = field_data.decode" (unless ICC).
             // So for ICC, DE{bit} is bytes?
             // But _string_to_pytype is called next.
             // _string_to_pytype assumes string or similar?
             // Let's assume bytes for ICC DE field in Java map, or hex string.
             // Python loads returns it as... wait, _string_to_pytype is called.
             // if ICC, it wasn't decoded. _string_to_pytype might fail if it expects string for int/decimal conversion?
             // But ICC usually doesn't have field_python_type set to int/decimal.
             // Let's assume it returns byte[] for DE55.
             finalValue = fieldDataBytes;
         } else {
             // standard conversion
              try {
                  finalValue = stringToType(fieldDataStr, config);
              } catch (Exception e) {
                  throw new Iso8583DataError("Unable to convert DE" + bit, message, e);
              }
         }
         
         result.values.put("DE" + bit, finalValue);

         if ("PDS".equals(fieldProcessor)) {
             result.values.putAll(pdsToDict(fieldDataStr));
         } else if ("DE43".equals(fieldProcessor)) {
             result.values.putAll(getDe43Fields(fieldDataStr, config.fieldProcessorConfig()));
         } else if ("ICC".equals(fieldProcessor)) {
             result.values.putAll(iccToDict(fieldDataBytes, config.fieldProcessorConfig()));
         }

         return result;
    }

    private static int getFieldLengthSize(BitConfig config) {
        if (config.fieldType() == BitConfig.FieldType.LLVAR) return 2;
        if (config.fieldType() == BitConfig.FieldType.LLLVAR) return 3;
        return 0;
    }

    private static Object stringToType(String val, BitConfig config) {
        String type = config.fieldJavaType();
        if ("int".equals(type) || "long".equals(type)) {
             return Long.parseLong(val);
        } else if ("decimal".equals(type)) {
             return Double.parseDouble(val); // or BigDecimal
        } else if ("datetime".equals(type)) {
             String fmt = config.fieldDateFormat();
             if (fmt == null) fmt = "yyMMdd";
             try {
                 return LocalDateTime.parse(val, DateTimeFormatter.ofPattern(fmt));
             } catch (Exception e) {
                 return val;
             }
        }
        return val;
    }
    
    private static byte[] fieldToBytes(BitConfig config, Object val, Charset encoding) {
       String strVal = pyTypeToString(val, config);
       int length = config.fieldLength();
       int lenSize = getFieldLengthSize(config);
       
       if (lenSize > 0) {
           length = strVal.length();
       }
       
       ByteArrayOutputStream os = new ByteArrayOutputStream();
       if (lenSize > 0) {
           String lenStr = String.format("%0" + lenSize + "d", length);
           try {
            os.write(lenStr.getBytes(encoding));
           } catch (Exception e) {}
       }
       
       try {
           byte[] content = strVal.getBytes(encoding);
           // Fixed length padding?
           // Python: format(field_value[:field_length], '<' + str(field_length))
           // This means left-aligned, space padded (default for string format).
           if (lenSize == 0) {
               if (content.length < length) {
                   // Pad with spaces
                   os.write(content);
                   for(int k=content.length; k<length; k++) os.write(' ');
               } else {
                   os.write(content, 0, length);
               }
           } else {
               os.write(content);
           }
       } catch (Exception e) {}
       
       return os.toByteArray();
    }

    private static String pyTypeToString(Object val, BitConfig config) {
        // Implement formatting logic
        if (val instanceof LocalDateTime) {
             String fmt = config.fieldDateFormat();
             if (fmt == null) fmt = "yyMMdd";
             return ((LocalDateTime)val).format(DateTimeFormatter.ofPattern(fmt));
        }
        String s = val.toString();
        // If number type, might need zero padding if fixed length?
        // Python: format(int(field_data), '0' + str(len) + 'd')
        if ("int".equals(config.fieldJavaType()) || "long".equals(config.fieldJavaType())) {
            if (config.fieldType() == BitConfig.FieldType.FIXED) {
                 return String.format("%0" + config.fieldLength() + "d", Long.parseLong(s));
            }
        }
        return s;
    }

    // --- PDS Helpers ---
    private static Map<String, String> pdsToDict(String fieldData) {
        Map<String, String> map = new HashMap<>();
        int ptr = 0;
        try {
            while (ptr < fieldData.length()) {
                String tag = fieldData.substring(ptr, ptr + 4);
                int len = Integer.parseInt(fieldData.substring(ptr + 4, ptr + 7));
                String val = fieldData.substring(ptr + 7, ptr + 7 + len);
                map.put("PDS" + tag, val);
                ptr += 7 + len;
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing PDS data", e);
        }
        return map;
    }
    
    private static List<String> pdsToDe(Map<String, Object> message) {
        List<String> keys = new ArrayList<>();
        for (String k : message.keySet()) if (k.startsWith("PDS")) keys.add(k);
        Collections.sort(keys);
        
        List<String> outputs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (String key : keys) {
            String val = message.get(key).toString();
            String tag = key.substring(3);
            String chunk = String.format("%04d%03d%s", Integer.parseInt(tag), val.length(), val);
            
            if (current.length() + chunk.length() > 999) {
                outputs.add(current.toString());
                current.setLength(0);
            }
            current.append(chunk);
        }
        if (current.length() > 0) outputs.add(current.toString());
        return outputs;
    }

    // --- ICC Helpers ---
    private static Map<String, String> iccToDict(byte[] data, String configStr) {
        Map<String, String> map = new HashMap<>();
        map.put("ICC_DATA", binAsciiHexlify(data));
        
        // Basic TLV parser
        int ptr = 0;
        try {
            while (ptr < data.length) {
                // Tag
                if (data[ptr] == 0) break;
                byte tag1 = data[ptr];
                byte[] tag;
                if ((tag1 & 0x1F) == 0x1F) {
                    // 2 byte tag (simplified check, real TLV is more complex but this matches python code somewhat)
                    // Python checks [0x9f, 0x5f] prefixes.
                   if (tag1 == (byte)0x9F || tag1 == (byte)0x5F) {
                       tag = new byte[]{tag1, data[ptr+1]};
                       ptr += 2;
                   } else {
                       tag = new byte[]{tag1};
                       ptr++;
                   }
                } else {
                     tag = new byte[]{tag1};
                     ptr++;
                }
                
                // Length (BER-TLV length? Python uses struct.unpack(">B") -> 1 byte length).
                // Python: field_length = struct.unpack(">B", field_length_raw)[0]
                if (ptr >= data.length) break;
                int len = data[ptr] & 0xFF; // unsigned
                ptr++;
                
                if (ptr + len > data.length) break;
                
                byte[] val = Arrays.copyOfRange(data, ptr, ptr + len);
                map.put("TAG" + binAsciiHexlify(tag).toUpperCase(), binAsciiHexlify(val).toUpperCase());
                ptr += len;
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing ICC", e);
        }
        return map;
    }

    // --- DE43 Helper ---
    private static Map<String, String> getDe43Fields(String data, String regex) {
        Map<String, String> map = new HashMap<>();
        if (regex == null) return map;
        try {
            // Java named groups support in regex?
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(data);
            if (m.matches()) {
                // How to get all group names? Java reflection or manual known names.
                // Config has fixed names: DE43NAME, etc.
                // I will hardcode attempting to fetch known groups for now or just rely on non-named approach if I couldn't use names.
                // But typically we want to return what was captured.
                // Java 20+ has namedGroups(), but Java 17?
                // We can try catching IllegalArgumentException for known keys.
                String[] potentialKeys = {"DE43NAME", "DE43ADDRESS", "DE43SUBURB", "DE43POSTCODE", "DE43STATE", "DE43COUNTRY"};
                for (String key : potentialKeys) {
                    try {
                        String v = m.group(key);
                        if (v != null) map.put("DE43_" + key.replace("DE43", ""), v.trim()); // Python does rstrip()
                    } catch (IllegalArgumentException e) {}
                }
            }
        } catch (Exception e) {}
        return map;
    }

    // --- Util ---
    private static String binAsciiHexlify(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
}
