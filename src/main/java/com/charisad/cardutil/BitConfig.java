package com.charisad.cardutil;

public record BitConfig(
    String fieldName,
    FieldType fieldType,    
    int fieldLength,
    String fieldProcessor,
    String fieldProcessorConfig,
    String fieldJavaType,
    String fieldDateFormat
) {
    public enum FieldType {
        FIXED, LLVAR, LLLVAR
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fieldName;
        private FieldType fieldType;
        private int fieldLength;
        private String fieldProcessor;
        private String fieldProcessorConfig;
        private String fieldJavaType;
        private String fieldDateFormat;

        public Builder fieldName(String fieldName) { this.fieldName = fieldName; return this; }
        public Builder fieldType(FieldType fieldType) { this.fieldType = fieldType; return this; }
        public Builder fieldLength(int fieldLength) { this.fieldLength = fieldLength; return this; }
        public Builder fieldProcessor(String fieldProcessor) { this.fieldProcessor = fieldProcessor; return this; }
        public Builder fieldProcessorConfig(String fieldProcessorConfig) { this.fieldProcessorConfig = fieldProcessorConfig; return this; }
        public Builder fieldJavaType(String fieldJavaType) { this.fieldJavaType = fieldJavaType; return this; }
        public Builder fieldDateFormat(String fieldDateFormat) { this.fieldDateFormat = fieldDateFormat; return this; }

        public BitConfig build() {
            return new BitConfig(fieldName, fieldType, fieldLength, fieldProcessor, fieldProcessorConfig, fieldJavaType, fieldDateFormat);
        }
    }
}
