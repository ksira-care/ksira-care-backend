package com.ksiracare.backend.export;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds a CSV file that opens cleanly in Excel and Google Sheets: UTF-8 with a byte-order mark
 * (so names and ₹ survive), every cell quoted, and cells that a spreadsheet would run as a
 * formula (starting with = + - @ or a control character) defused with a leading apostrophe —
 * client-entered text ends up in these files.
 */
public final class CsvWriter {

    /** Shown for a value that doesn't exist. */
    public static final String NONE = "-";

    private static final String BYTE_ORDER_MARK = "﻿";
    private static final String FORMULA_STARTS = "=+-@\t\r";

    private final StringBuilder csv = new StringBuilder(BYTE_ORDER_MARK);

    public CsvWriter row(List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(cell(cells.get(i)));
        }
        csv.append("\r\n");
        return this;
    }

    public byte[] toBytes() {
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String cell(String value) {
        String text = value == null || value.isBlank() ? NONE : value;
        if (!text.equals(NONE) && FORMULA_STARTS.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
