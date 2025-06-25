package com.vengalsas.core.conciliation.infrastructure.adapter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
      boolean foundMovements = false;

      for (Row row : sheet) {
        if (!foundMovements) {
          if (cellText(row.getCell(0)).toLowerCase().contains("movimientos")) {
            foundMovements = true;
          }
          continue;
        }

        // Fila de encabezado de movimientos: FECHA | DESCRIPCIÓN | ...
        if (cellText(row.getCell(0)).equalsIgnoreCase("FECHA")) {
          continue;
        }

        try {
          String rawDate = cellText(row.getCell(0));
          String description = cellText(row.getCell(1));
          String amountStr = cellText(row.getCell(4)).replace(",", "").trim(); // conserva los decimales

          if (rawDate.isEmpty() || description.isEmpty() || amountStr.isEmpty())
            continue;

          LocalDate date = parseDate(rawDate);
          BigDecimal amount = new BigDecimal(amountStr);

          TransactionType type = guessTypeFromDescription(description);

          Transaction transaction = Transaction.builder()
              .date(date)
              .description(description)
              .amount(amount.abs()) // usar valor absoluto
              .transactionType(type)
              .source(SourceSystem.BANCOLUMBIA)
              .reference("")
              .build();

          transactions.add(transaction);
        } catch (Exception e) {
          System.err.println("Error parsing row " + row.getRowNum() + ": " + e.getMessage());
        }
      }
    }

    return transactions;
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
      case FORMULA -> String.valueOf(cell.getNumericCellValue());
      default -> "";
    };
  }

  private TransactionType guessTypeFromDescription(String desc) {
    String d = desc.toLowerCase();
    if (d.contains("pago") || d.contains("impto") || d.contains("cuota") || d.contains("comision")) {
      return TransactionType.DEBIT;
    }
    return TransactionType.CREDIT;
  }
}
