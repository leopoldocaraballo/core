package com.vengalsas.core.conciliation.infrastructure.adapter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.domain.model.TransactionType;

@Component
public class BancolombiaExcelReader {

  private static final int ASSUMED_YEAR = 2025;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

  public List<Transaction> read(InputStream inputStream) throws Exception {
    List<Transaction> transactions = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      int headerRowIndex = -1;
      Map<String, Integer> columnMap = new HashMap<>();

      for (Row row : sheet) {
        if (headerRowIndex == -1 && row != null) {
          for (Cell cell : row) {
            String text = cellText(cell).toLowerCase();
            if (text.contains("fecha") || text.contains("descripción") || text.contains("valor")) {
              headerRowIndex = row.getRowNum();
              break;
            }
          }
          continue;
        }

        if (row == null || row.getRowNum() <= headerRowIndex)
          continue;

        if (columnMap.isEmpty()) {
          for (Cell cell : sheet.getRow(headerRowIndex)) {
            String name = cellText(cell).trim().toLowerCase();
            columnMap.put(name, cell.getColumnIndex());
          }
        }

        try {
          String rawDate = getCellValue(row, columnMap, "fecha");
          String description = getCellValue(row, columnMap, "descripción");
          String amountStr = getCellValue(row, columnMap, "valor");

          if (rawDate.isEmpty() || description.isEmpty() || amountStr.isEmpty())
            continue;

          LocalDate date = parseDate(rawDate);
          BigDecimal rawAmount = new BigDecimal(amountStr.replace(",", "").trim());
          BigDecimal amount = rawAmount.abs();

          TransactionType type = rawAmount.signum() < 0 ? TransactionType.DEBIT : guessTypeFromDescription(description);

          Transaction tx = Transaction.builder()
              .date(date)
              .description(description)
              .amount(amount)
              .transactionType(type)
              .source(SourceSystem.BANCOLOMBIA)
              .build();

          transactions.add(tx);
        } catch (Exception e) {
          System.err.println("Error parsing row " + row.getRowNum() + ": " + e.getMessage());
        }
      }
    }

    return transactions;
  }

  private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
    Integer index = columnMap.get(columnName.toLowerCase());
    return (index != null) ? cellText(row.getCell(index)) : "";
  }

  private LocalDate parseDate(String raw) {
    if (!raw.contains("/"))
      throw new IllegalArgumentException("Invalid date format: " + raw);
    String[] parts = raw.split("/");
    int day = Integer.parseInt(parts[0]);
    int month = Integer.parseInt(parts[1]);
    return LocalDate.of(ASSUMED_YEAR, month, day);
  }

  private String cellText(Cell cell) {
    if (cell == null)
      return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> DateUtil.isCellDateFormatted(cell)
          ? cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER)
          : String.valueOf(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
  }

  private TransactionType guessTypeFromDescription(String desc) {
    String d = desc.toLowerCase();
    if (d.contains("pago") || d.contains("impto") || d.contains("cuota") || d.contains("comisión")
        || d.contains("desembolso")) {
      return TransactionType.DEBIT;
    }
    return TransactionType.CREDIT;
  }
}
