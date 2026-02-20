package com.charisad.cardutil;

import java.io.*;
import java.util.*;

public class GenerateTestIpm {
    public static void main(String[] args) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("MTI", "1144");
        data.put("DE2", "5555444433332222");
        data.put("DE12", "260219120000"); // YYMMDDHHMMSS
        
        try (OutputStream os = new FileOutputStream("test.ipm");
             MciIpm.IpmWriter writer = new MciIpm.IpmWriter(os, true)) {
            writer.write(data);
        }
        System.out.println("Generated test.ipm");
    }
}
