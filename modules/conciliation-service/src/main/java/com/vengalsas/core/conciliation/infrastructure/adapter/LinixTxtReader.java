package com.vengalsas.core.conciliation.infrastructure.adapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.domain.model.TransactionType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LinixTxtReader {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

  public List<Transaction> read(InputStream inputStream) {
    List<Transaction> transactions = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("Concepto") || !line.contains("\t"))
          continue;

        String[] parts = line.split("\t");
        if (parts.length < 19)
          continue;

        try {
          String dateStr = parts[11].trim();
          String description = parts[15].trim();
          String debitStr = parts[17].trim().replace(",", "");
          String creditStr = parts[18].trim().replace(",", "");

          LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
          BigDecimal debit = debitStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(debitStr);
          BigDecimal credit = creditStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(creditStr);

          BigDecimal amount;
          TransactionType type;

          if (debit.compareTo(BigDecimal.ZERO) > 0) {
            amount = debit.abs();
            type = TransactionType.DEBIT;
          } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
            amount = credit.abs();
            type = TransactionType.CREDIT;
          } else {
            continue; // Transacción sin valor monetario válido
          }

          Transaction transaction = Transaction.builder()
              .date(date)
              .description(description)
              .amount(amount)
              .transactionType(type)
              .source(SourceSystem.LINIX)
              .reference("")
              .build();

          transactions.add(transaction);

        } catch (Exception e) {
          log.warn("Línea ignorada por error de formato o conversión: {}", line);
        }
      }

    } catch (Exception e) {
      log.error("Error al leer el archivo Linix: {}", e.getMessage(), e);
    }

    return transactions;
  }
}
