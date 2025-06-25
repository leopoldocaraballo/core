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

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("Concepto") || !line.contains("\t"))
          continue;

        String[] parts = line.split("\t");

        try {
          String dateStr = parts[11].trim();
          String description = parts[15].trim();
          String debitStr = parts[17].trim().replace(",", "").replace(".00", "");
          String creditStr = parts[18].trim().replace(",", "").replace(".00", "");

          LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);

          BigDecimal amount;
          TransactionType type;

          if (!creditStr.isEmpty() && new BigDecimal(creditStr).compareTo(BigDecimal.ZERO) > 0) {
            amount = new BigDecimal(creditStr);
            type = TransactionType.DEBIT;
          } else if (!debitStr.isEmpty() && new BigDecimal(debitStr).compareTo(BigDecimal.ZERO) > 0) {
            amount = new BigDecimal(debitStr);
            type = TransactionType.CREDIT;
          } else {
            continue; // No hay valor válido
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
          log.warn("Línea ignorada por error de formato: {}", line);
        }
      }
    } catch (Exception e) {
      log.error("Error al leer el archivo Linix: {}", e.getMessage(), e);
    }

    return transactions;
  }
}
