package com.charisad.cardutil;

import java.util.HashMap;
import java.util.Map;
import static com.charisad.cardutil.BitConfig.FieldType.*;


public class Config {
    public static final Map<Integer, BitConfig> DEFAULT_BIT_CONFIG = new HashMap<>();

    static {
        DEFAULT_BIT_CONFIG.put(1, BitConfig.builder()
                .fieldName("Bitmap secondary").fieldType(FIXED).fieldLength(8).build());
        DEFAULT_BIT_CONFIG.put(2, BitConfig.builder()
                .fieldName("PAN").fieldType(LLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(3, BitConfig.builder()
                .fieldName("Processing code").fieldType(FIXED).fieldLength(6).build());
        DEFAULT_BIT_CONFIG.put(4, BitConfig.builder()
                .fieldName("Amount transaction").fieldType(FIXED).fieldLength(12).fieldJavaType("long").build());
        DEFAULT_BIT_CONFIG.put(5, BitConfig.builder()
                .fieldName("Amount, Reconciliation").fieldType(FIXED).fieldLength(12).fieldJavaType("long").build());
        DEFAULT_BIT_CONFIG.put(6, BitConfig.builder()
                .fieldName("Amount, Cardholder billing").fieldType(FIXED).fieldLength(12).fieldJavaType("long").build());
        DEFAULT_BIT_CONFIG.put(9, BitConfig.builder()
                .fieldName("Conversion rate, Reconciliation").fieldType(FIXED).fieldLength(8).fieldJavaType("long").build());
        DEFAULT_BIT_CONFIG.put(10, BitConfig.builder()
                .fieldName("Conversion rate, Cardholder billing").fieldType(FIXED).fieldLength(8).fieldJavaType("long").build());
        DEFAULT_BIT_CONFIG.put(12, BitConfig.builder()
                .fieldName("Date/Time local transaction").fieldType(FIXED).fieldLength(12)
                .fieldJavaType("datetime").fieldDateFormat("yyMMddHHmmss").build());
        DEFAULT_BIT_CONFIG.put(14, BitConfig.builder()
                .fieldName("Expiration date").fieldType(FIXED).fieldLength(4).build());
        DEFAULT_BIT_CONFIG.put(22, BitConfig.builder()
                .fieldName("Point of service data code").fieldType(FIXED).fieldLength(12).build());
        DEFAULT_BIT_CONFIG.put(23, BitConfig.builder()
                .fieldName("Card sequence number").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(24, BitConfig.builder()
                .fieldName("Function code").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(25, BitConfig.builder()
                .fieldName("Message reason code").fieldType(FIXED).fieldLength(4).build());
        DEFAULT_BIT_CONFIG.put(26, BitConfig.builder()
                .fieldName("Card acceptor business code").fieldType(FIXED).fieldLength(4).fieldJavaType("int").build());
        DEFAULT_BIT_CONFIG.put(30, BitConfig.builder()
                .fieldName("Amounts, original").fieldType(FIXED).fieldLength(24).build());
        DEFAULT_BIT_CONFIG.put(31, BitConfig.builder()
                .fieldName("Acquirer reference data").fieldType(LLVAR).fieldLength(23).build());
        DEFAULT_BIT_CONFIG.put(32, BitConfig.builder()
                .fieldName("Acquiring institution ID code").fieldType(LLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(33, BitConfig.builder()
                .fieldName("Forwarding institution ID code").fieldType(LLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(37, BitConfig.builder()
                .fieldName("Retrieval reference number").fieldType(FIXED).fieldLength(12).build());
        DEFAULT_BIT_CONFIG.put(38, BitConfig.builder()
                .fieldName("Approval code").fieldType(FIXED).fieldLength(6).build());
        DEFAULT_BIT_CONFIG.put(40, BitConfig.builder()
                .fieldName("Service code").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(41, BitConfig.builder()
                .fieldName("Card acceptor terminal ID").fieldType(FIXED).fieldLength(8).build());
        DEFAULT_BIT_CONFIG.put(42, BitConfig.builder()
                .fieldName("Card acceptor Id").fieldType(FIXED).fieldLength(15).build());
        DEFAULT_BIT_CONFIG.put(43, BitConfig.builder()
                .fieldName("Card acceptor name/location").fieldType(LLVAR).fieldLength(0)
                .fieldProcessor("DE43")
                // Simplified regex for Java, avoiding named groups if not strictly supported in same way, 
                // but Java 7+ supports named groups (?<name>...). configuration string format might need adjustment in implementation.
                // Keeping it as string for now.
                .fieldProcessorConfig("(?<DE43NAME>.+?) *\\\\(?<DE43ADDRESS>.+?) *\\\\(?<DE43SUBURB>.+?) *\\\\"
                        + "(?<DE43POSTCODE>.{10})(?<DE43STATE>.{3})(?<DE43COUNTRY>\\\\S{3})$")
                .build());
        DEFAULT_BIT_CONFIG.put(48, BitConfig.builder()
                .fieldName("Additional data").fieldType(LLLVAR).fieldLength(0).fieldProcessor("PDS").build());
        DEFAULT_BIT_CONFIG.put(49, BitConfig.builder()
                .fieldName("Currency code, Transaction").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(50, BitConfig.builder()
                .fieldName("Currency code, Reconciliation").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(51, BitConfig.builder()
                .fieldName("Currency code, Cardholder billing").fieldType(FIXED).fieldLength(3).build());
        DEFAULT_BIT_CONFIG.put(54, BitConfig.builder()
                .fieldName("Amounts, additional").fieldType(LLLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(55, BitConfig.builder()
                .fieldName("ICC system related data").fieldType(LLLVAR).fieldLength(255)
                .fieldProcessor("ICC").fieldProcessorConfig("on_error=WARN").build());
        DEFAULT_BIT_CONFIG.put(62, BitConfig.builder()
                .fieldName("Additional data 2").fieldType(LLLVAR).fieldLength(0).fieldProcessor("PDS").build());
        DEFAULT_BIT_CONFIG.put(63, BitConfig.builder()
                .fieldName("Transaction lifecycle Id").fieldType(LLLVAR).fieldLength(16).build());
        DEFAULT_BIT_CONFIG.put(71, BitConfig.builder()
                .fieldName("Message number").fieldType(FIXED).fieldLength(8).fieldJavaType("int").build());
        DEFAULT_BIT_CONFIG.put(72, BitConfig.builder()
                .fieldName("Data record").fieldType(LLLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(73, BitConfig.builder()
                .fieldName("Date, Action").fieldType(FIXED).fieldLength(6).build());
        DEFAULT_BIT_CONFIG.put(93, BitConfig.builder()
                .fieldName("Transaction destination institution ID").fieldType(LLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(94, BitConfig.builder()
                .fieldName("Transaction originator institution ID").fieldType(LLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(95, BitConfig.builder()
                .fieldName("Card issuer reference data").fieldType(LLVAR).fieldLength(10).build());
        DEFAULT_BIT_CONFIG.put(100, BitConfig.builder()
                .fieldName("Receiving institution ID").fieldType(LLVAR).fieldLength(11).build());
        DEFAULT_BIT_CONFIG.put(105, BitConfig.builder()
                .fieldName("Multi-Use Transaction Identification Data").fieldType(LLLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(111, BitConfig.builder()
                .fieldName("Amount, currency conversion assignment").fieldType(LLLVAR).fieldLength(0).build());
        DEFAULT_BIT_CONFIG.put(123, BitConfig.builder()
                .fieldName("Additional data 3").fieldType(LLLVAR).fieldLength(0).fieldProcessor("PDS").build());
        DEFAULT_BIT_CONFIG.put(124, BitConfig.builder()
                .fieldName("Additional data 4").fieldType(LLLVAR).fieldLength(0).fieldProcessor("PDS").build());
        DEFAULT_BIT_CONFIG.put(125, BitConfig.builder()
                .fieldName("Additional data 5").fieldType(LLLVAR).fieldLength(0).fieldProcessor("PDS").build());
        DEFAULT_BIT_CONFIG.put(127, BitConfig.builder()
                .fieldName("Network data").fieldType(LLLVAR).fieldLength(0).build());
    }
}
