package com.smartagriculture.knowledgeservice.importer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    @Test
    void parsesHeaderAndRows_blankCellsBecomeNull() {
        List<CsvParser.CsvRow> rows = CsvParser.parse("crop,target,doseValue\nRICE,blast,1.5\nRICE,,\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).number()).isEqualTo(2);
        assertThat(rows.get(0).values()).containsEntry("crop", "RICE").containsEntry("target", "blast").containsEntry("doseValue", "1.5");
        assertThat(rows.get(1).values().get("target")).isNull();
        assertThat(rows.get(1).values().get("doseValue")).isNull();
    }

    @Test
    void quotedFields_mayContainCommasNewlinesAndDoubledQuotes() {
        String csv = "nameEn,symptoms\r\n\"Leaf spot\",\"Spots, brown\nwith \"\"grey\"\" centres\"\r\n";

        List<CsvParser.CsvRow> rows = CsvParser.parse(csv);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).values().get("symptoms")).isEqualTo("Spots, brown\nwith \"grey\" centres");
    }

    @Test
    void excelUtf8Bom_isStripped_andBanglaIsKept() {
        List<CsvParser.CsvRow> rows = CsvParser.parse("﻿nameEn,nameBn\nBlast,ব্লাস্ট\n");

        assertThat(rows.get(0).values()).containsEntry("nameEn", "Blast").containsEntry("nameBn", "ব্লাস্ট");
    }

    @Test
    void emptyLinesAreSkipped_butRowNumbersMatchTheFile() {
        List<CsvParser.CsvRow> rows = CsvParser.parse("crop\nRICE\n\nWHEAT\n");

        assertThat(rows).extracting(CsvParser.CsvRow::number).containsExactly(2, 4);
    }

    @Test
    void tooManyColumns_orUnclosedQuote_isRejected() {
        assertThatThrownBy(() -> CsvParser.parse("a,b\n1,2,3\n")).hasMessageContaining("Row 2 has 3 columns");
        assertThatThrownBy(() -> CsvParser.parse("a\n\"open\n")).hasMessageContaining("Unclosed quote");
    }

    @Test
    void emptyInput_givesNoRows() {
        assertThat(CsvParser.parse("")).isEmpty();
    }
}
