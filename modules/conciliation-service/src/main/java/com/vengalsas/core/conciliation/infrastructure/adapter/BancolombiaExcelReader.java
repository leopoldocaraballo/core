package com.vengalsas.core.conciliation.infrastructure.adapter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.vengalsas.core.conciliation.domain.model.BancolombiaStatement;
import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.domain.model.TransactionType;

@Component
public class BancolombiaExcelReader {

  private static final int HEADER_ROW_INDEX = 13; // Fila 14 en Excel (0-based)
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

  public BancolombiaStatement read(InputStream inputStream) throws Exception {
    List<Transaction> transactions = new ArrayList<>();
    BigDecimal startingBalance = BigDecimal.ZERO;
    BigDecimal endingBalance = BigDecimal.ZERO;
    BigDecimal totalDebits = BigDecimal.ZERO;
    BigDecimal totalCredits = BigDecimal.ZERO;

    try (Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);

      // Extraer saldos de las filas superiores
      for (int i = 8; i <= 12; i++) {
        Row row = sheet.getRow(i);
        if (row == null)
          continue;

        for (Cell cell : row) {
          String val = cellText(cell).toLowerCase();
          if (val.contains("saldo anterior")) {
            startingBalance = extractNumericBelowOrRight(sheet, cell);
          } else if (val.contains("total abonos")) {
            totalCredits = extractNumericBelowOrRight(sheet, cell);
          } else if (val.contains("total cargos")) {
            totalDebits = extractNumericBelowOrRight(sheet, cell);
          } else if (val.contains("saldo actual")) {
            endingBalance = extractNumericBelowOrRight(sheet, cell);
          }
        }
      }

      // Leer transacciones desde la fila 15
      for (int i = HEADER_ROW_INDEX + 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null)
          continue;

        String rawDate = cellText(row.getCell(0));
        String description = cellText(row.getCell(1));
        String amountStr = cellText(row.getCell(4));

        if (rawDate.isEmpty() || description.isEmpty() || amountStr.isEmpty())
          continue;

        try {
          LocalDate date = parseDate(rawDate);
          BigDecimal rawAmount = new BigDecimal(amountStr.replace(",", "").trim());
          BigDecimal amount = rawAmount.abs();
          TransactionType type = rawAmount.signum() < 0 ? TransactionType.DEBIT : TransactionType.CREDIT;

          Transaction tx = Transaction.builder()
              .date(date)
              .description(description)
              .amount(amount)
              .transactionType(type)
              .source(SourceSystem.BANCOLOMBIA)
              .build();

          transactions.add(tx);
        } catch (Exception e) {
          System.err.println("[WARN] Error parsing row " + i + ": " + e.getMessage());
        }
      }
    }

    return BancolombiaStatement.builder()
        .transactions(transactions)
        .startingBalance(startingBalance)
        .endingBalance(endingBalance)
        .totalCredits(totalCredits)
        .totalDebits(totalDebits)
        .build();
  }

  private String cellText(Cell cell) {
    if (cell == null)
      return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> DateUtil.isCellDateFormatted(cell)
          ? cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER)
          : BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        try {
          if (cell.getCachedFormulaResultType() == CellType.NUMERIC)
            yield BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
          else
            yield cell.getStringCellValue();
        } catch (Exception e) {
          yield "";
        }
      }
      default -> "";
    };
  }

  private BigDecimal extractNumericBelowOrRight(Sheet sheet, Cell labelCell) {
    Row row = labelCell.getRow();
    int colIndex = labelCell.getColumnIndex();
    int rowIndex = row.getRowNum();

    // 1. Buscar en celda de abajo
    if (sheet.getRow(rowIndex + 1) != null) {
      Cell below = sheet.getRow(rowIndex + 1).getCell(colIndex);
      try {
        String text = cellText(below).replace(",", "").trim();
        return new BigDecimal(text);
      } catch (Exception ignored) {
      }
    }

    // 2. Fallback: celda a la derecha
    Cell right = row.getCell(colIndex + 1);
    try {
      String text = cellText(right).replace(",", "").trim();
      return new BigDecimal(text);
    } catch (Exception ignored) {
    }

    return BigDecimal.ZERO;
  }

  private LocalDate parseDate(String raw) {
    String[] parts = raw.split("/");
    int day = Integer.parseInt(parts[0]);
    int month = Integer.parseInt(parts[1]);
    int year = (parts.length > 2) ? Integer.parseInt(parts[2]) : LocalDate.now().getYear();
    return LocalDate.of(year, month, day);
  }
}
