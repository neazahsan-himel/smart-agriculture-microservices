package com.smartagriculture.knowledgeservice.importer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal RFC 4180 CSV parser: first row is the header; quoted fields may contain commas,
 * newlines and doubled quotes. Blank cells become null. A UTF-8 BOM (Excel's "CSV UTF-8")
 * is stripped. No external dependency needed for the pilot's small files.
 */
public final class CsvParser {

    private static final char BOM = (char) 0xFEFF;

    private CsvParser() {
    }

    /** A data row: its record number in the file (header = 1) and its cells keyed by header name. */
    public record CsvRow(int number, Map<String, String> values) {
    }

    public static List<CsvRow> parse(String csv) {
        List<List<String>> records = readRecords(!csv.isEmpty() && csv.charAt(0) == BOM ? csv.substring(1) : csv);
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> header = records.get(0).stream().map(String::trim).toList();
        List<CsvRow> rows = new ArrayList<>();
        for (int r = 1; r < records.size(); r++) {
            List<String> fields = records.get(r);
            if (fields.stream().allMatch(f -> f.isBlank())) {
                continue; // skip empty lines
            }
            if (fields.size() > header.size()) {
                throw new IllegalArgumentException("Row " + (r + 1) + " has " + fields.size()
                        + " columns but the header has " + header.size());
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                String value = c < fields.size() ? fields.get(c).trim() : "";
                row.put(header.get(c), value.isEmpty() ? null : value);
            }
            rows.add(new CsvRow(r + 1, row));
        }
        return rows;
    }

    private static List<List<String>> readRecords(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                records.add(fields);
                fields = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quote in CSV");
        }
        if (field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            records.add(fields);
        }
        return records;
    }
}
