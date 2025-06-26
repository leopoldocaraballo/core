package com.vengalsas.core.conciliation.application.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.infrastructure.adapter.BancolombiaExcelReader;
import com.vengalsas.core.conciliation.infrastructure.adapter.LinixTxtReader;
import com.vengalsas.core.conciliation.web.dto.ConciliationResultDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

  private final BancolombiaExcelReader bancolombiaExcelReader;
  private final LinixTxtReader linixTxtReader;

  /**
   * Reads and normalizes transactions from Bancolombia and Linix files.
   */
  public List<Transaction> readAndNormalize(MultipartFile bankFile, MultipartFile accountingFile) throws Exception {
    try (
        InputStream bankInput = bankFile.getInputStream();
        InputStream accountingInput = accountingFile.getInputStream()) {

      List<Transaction> bancolombia = bancolombiaExcelReader.read(bankInput);
      List<Transaction> linix = linixTxtReader.read(accountingInput);

      log.info("Bancolombia transactions loaded: {}", bancolombia.size());
      log.info("Linix transactions loaded: {}", linix.size());

      List<Transaction> all = new ArrayList<>(bancolombia);
      all.addAll(linix);
      return all;
    }
  }

  /**
   * Performs reconciliation between Linix and Bancolombia transactions.
   */
  public List<ConciliationResultDTO> reconcileTransactions(ReconciliationRequestDTO request) {
    List<Transaction> linixTxs = request.getLinixTransactions();
    List<Transaction> bancoTxs = request.getBancolombiaTransactions();

    List<ConciliationResultDTO> results = new ArrayList<>();
    Set<Integer> matchedIndexes = new HashSet<>();

    for (Transaction linix : linixTxs) {
      boolean matched = false;

      for (int i = 0; i < bancoTxs.size(); i++) {
        if (matchedIndexes.contains(i))
          continue;

        Transaction banco = bancoTxs.get(i);

        if (isMatch(linix, banco)) {
          results.add(ConciliationResultDTO.builder()
              .linixTransaction(linix)
              .bancolombiaTransaction(banco)
              .matched(true)
              .build());
          matchedIndexes.add(i);
          matched = true;
          log.debug("Match: LINIX [{}] ↔ BANCO [{}]", linix.getReference(), banco.getReference());
          break;
        }
      }

      if (!matched) {
        results.add(ConciliationResultDTO.builder()
            .linixTransaction(linix)
            .bancolombiaTransaction(null)
            .matched(false)
            .build());
        log.debug("Unmatched LINIX transaction [{}]", linix.getReference());
      }
    }

    // Agrega transacciones Bancolombia no conciliadas
    for (int i = 0; i < bancoTxs.size(); i++) {
      if (!matchedIndexes.contains(i)) {
        Transaction banco = bancoTxs.get(i);
        results.add(ConciliationResultDTO.builder()
            .linixTransaction(null)
            .bancolombiaTransaction(banco)
            .matched(false)
            .build());
        log.debug("Unmatched BANCO transaction [{}]", banco.getReference());
      }
    }

    log.info("Conciliation completed. Total results: {}", results.size());
    return results;
  }

  /**
   * Matching logic: currently strict by date and amount.
   * Can be extended with fuzzy logic (±1 día, referencia parcial, etc.).
   */
  private boolean isMatch(Transaction a, Transaction b) {
    boolean sameDate = a.getDate().isEqual(b.getDate());
    boolean similarAmount = a.getAmount().subtract(b.getAmount()).abs().compareTo(BigDecimal.ONE) <= 0;
    return sameDate && similarAmount;
  }
}
