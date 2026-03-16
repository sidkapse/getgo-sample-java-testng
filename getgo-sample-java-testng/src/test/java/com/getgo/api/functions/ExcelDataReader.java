package com.getgo.api.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelDataReader — reads test data rows from .xlsx files.
 *
 * CODING CONVENTIONS:
 * - Each sheet name corresponds to one test scenario group.
 * - Row 0 is always the header row (column names).
 * - Data rows start from row 1.
 * - Returns List<Map<String, String>> — each Map is one row keyed by header name.
 *
 * EXCEL FILE LOCATION: test-data/getgo_test_data.xlsx
 *
 * USAGE PATTERN IN TESTS:
 *   List<Map<String, String>> rows = ExcelDataReader.readSheet("CreateBooking");
 *   for (Map<String, String> row : rows) {
 *       String passengerId = row.get("passenger_id");
 *   }
 */
public class ExcelDataReader {

    private static final Logger log = LogManager.getLogger(ExcelDataReader.class);
    private static final String DATA_FILE_PATH = "test-data/getgo_test_data.xlsx";

    /**
     * Reads all data rows from the given sheet name.
     *
     * @param sheetName the Excel sheet tab name (e.g. "CreateBooking")
     * @return list of row maps — each map keys column header → cell value as String
     */
    public static List<Map<String, String>> readSheet(String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        log.info("Reading Excel sheet={} from file={}", sheetName, DATA_FILE_PATH);

        try (FileInputStream fis = new FileInputStream(DATA_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + sheetName);
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }
            log.info("Headers found: {}", headers);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(j), getCellValueAsString(cell));
                }
                data.add(rowMap);
            }

        } catch (IOException e) {
            log.error("Failed to read Excel file: {}", e.getMessage());
            throw new RuntimeException("Excel read error: " + e.getMessage(), e);
        }

        log.info("Loaded {} rows from sheet={}", data.size(), sheetName);
        return data;
    }

    /** Converts any cell type to a trimmed String value. */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }
}
