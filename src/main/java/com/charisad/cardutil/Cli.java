package com.charisad.cardutil;

import com.charisad.cardutil.MciIpm.IpmReader;
import com.charisad.cardutil.MciIpm.IpmWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class Cli {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        try {
            switch (command) {
                case "ipm2csv":
                    ipm2csv(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "csv2ipm":
                    csv2ipm(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "help":
                default:
                    printUsage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java com.charisad.cardutil.Cli <command> [args]");
        System.out.println("Commands:");
        System.out.println("  ipm2csv <input_ipm> [-o <output_csv>]");
        System.out.println("  csv2ipm <input_csv> [-o <output_ipm>]");
    }

    private static void ipm2csv(String[] args) throws IOException {
        String input = null;
        String output = null;
        
        for (int i = 0; i < args.length; i++) {
            if ("-o".equals(args[i]) && i + 1 < args.length) {
                output = args[i + 1];
                i++;
            } else if (input == null) {
                input = args[i];
            }
        }

        if (input == null) {
            System.out.println("Error: Input file required");
            return;
        }
        if (output == null) {
            output = input + ".csv";
        }

        System.out.println("Converting IPM " + input + " to CSV " + output);

        try (InputStream is = Files.newInputStream(new File(input).toPath());
             IpmReader reader = new IpmReader(is, true, StandardCharsets.ISO_8859_1, Config.DEFAULT_BIT_CONFIG);
             BufferedWriter writer = Files.newBufferedWriter(new File(output).toPath());
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader().build())) {

            // We need to collect all possible headers first? 
            // Or just dump what we find. 
            // Python version collects headers from all records if not specified?
            // "dicts_to_csv" in python does collection if field_list is None.
            // Since we are streaming, we might miss headers if we don't scan first.
            // But double reading IPM is expensive.
            // Let's use the columns defined in Config or a superset if possible.
            // Ideally we should do a two-pass or just dump generic columns.
            // For now, let's just dump known columns from Config + explicit ones found?
            // No, CSVPrinter needs headers at start.
            
            // Let's use a standard list of ISO fields 1-128 + MTI
            List<String> headers = new ArrayList<>();
            headers.add("MTI");
            for (int i = 1; i <= 128; i++) {
                headers.add("DE" + i);
            }
             // Add PDS/ICC subfields if we want to extract them? 
             // The Python code seems to flatten dicts.
             // For a simple start, let's just dump MTI + DEs.
             
             // Wait, we need to print header first.
             csvPrinter.printRecord(headers);

             for (Map<String, Object> record : reader) {
                 List<Object> values = new ArrayList<>();
                 for (String h : headers) {
                     values.add(record.getOrDefault(h, ""));
                 }
                 csvPrinter.printRecord(values);
             }
        }
    }

    private static void csv2ipm(String[] args) throws IOException {
        String input = null;
        String output = null;

        for (int i = 0; i < args.length; i++) {
            if ("-o".equals(args[i]) && i + 1 < args.length) {
                output = args[i + 1];
                i++;
            } else if (input == null) {
                input = args[i];
            }
        }

        if (input == null) {
            System.out.println("Error: Input file required");
            return;
        }
        if (output == null) {
            output = input.replace(".csv", "") + ".ipm";
        }

        System.out.println("Converting CSV " + input + " to IPM " + output);

        try (Reader reader = Files.newBufferedReader(new File(input).toPath());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());
             OutputStream os = Files.newOutputStream(new File(output).toPath());
             IpmWriter ipmWriter = new IpmWriter(os, true, StandardCharsets.ISO_8859_1, Config.DEFAULT_BIT_CONFIG)) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, Object> record = new HashMap<>();
                Map<String, String> row = csvRecord.toMap();
                
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    if (val != null && !val.isEmpty()) {
                        record.put(key, val);
                    }
                }
                ipmWriter.write(record);
            }
        }
    }
}
