package com.ksiracare.backend.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    private static String write(String... cells) {
        return new String(new CsvWriter().row(Arrays.asList(cells)).toBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void startsWithAByteOrderMarkSoExcelReadsUtf8() {
        assertThat(write("Ishita", "₹899")).isEqualTo("﻿\"Ishita\",\"₹899\"\r\n");
    }

    @Test
    void quotesEveryCellAndDoublesInnerQuotes() {
        assertThat(write("Said \"hi\", then left")).endsWith("\"Said \"\"hi\"\", then left\"\r\n");
    }

    @Test
    void showsADashForMissingValues() {
        assertThat(write(null, "", "  ")).endsWith("\"-\",\"-\",\"-\"\r\n");
    }

    @Test
    void defusesCellsASpreadsheetWouldRunAsFormulas() {
        String row = write("=HYPERLINK(\"http://evil\")", "+1", "-2", "@SUM(A1)");

        assertThat(row).contains("\"'=HYPERLINK(\"\"http://evil\"\")\"", "\"'+1\"", "\"'-2\"", "\"'@SUM(A1)\"");
        assertThat(write("-")).as("our own 'no value' dash").endsWith("\"-\"\r\n");
    }

    @Test
    void writesOneLinePerRow() {
        String csv = new String(new CsvWriter().row(List.of("a")).row(List.of("b")).toBytes(), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo("﻿\"a\"\r\n\"b\"\r\n");
    }
}
