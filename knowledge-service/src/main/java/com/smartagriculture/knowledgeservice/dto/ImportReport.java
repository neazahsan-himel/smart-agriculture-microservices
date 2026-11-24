package com.smartagriculture.knowledgeservice.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Result of a CSV import: rows are imported independently, so one bad row doesn't block the rest. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReport {

    private String type;
    private int totalRows;
    private int imported;
    private int failed;

    @Builder.Default
    private List<RowError> errors = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;        // spreadsheet row number (header = row 1)
        private String message;
    }
}
