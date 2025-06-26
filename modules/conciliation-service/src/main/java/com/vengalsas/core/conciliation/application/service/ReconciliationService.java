package com.vengalsas.core.conciliation.application.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vengalsas.core.conciliation.domain.model.BancolombiaStatement;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.infrastructure.adapter.BancolombiaExcelReader;
import com.vengalsas.core.conciliation.infrastructure.adapter.LinixTxtReader;
import com.vengalsas.core.conciliation.web.dto.ConciliationResultDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationRequestDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationResponseDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationSummaryDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

  private final BancolombiaExcelReader bancolombiaExcelReader;
  private final LinixTxtReader linixTxtReader;

  private BancolombiaStatement lastBancolombiaStatement;

  public List<Transaction> readAndNormalize(MultipartFile bankFile, MultipartFile accountingFile) throws Exception {
    try (
        InputStream bankInput = bankFile.getInputStream();
        InputStream accountingInput = accountingFile.getInputStream()) {

      this.lastBancolombiaStatement = bancolombiaExcelReader.read(bankInput);
      List<Transaction> bancolombia = lastBancolombiaStatement.getTransactions();
      List<Transaction> linix = linixTxtReader.read(accountingInput);

      log.info("Bancolombia transactions loaded: {}", bancolombia.size());
      log.info("Linix transactions loaded: {}", linix.size());

      List<Transaction> all = new ArrayList<>(bancolombia);
      all.addAll(linix);
      return all;
    }
  }

  public ReconciliationResponseDTO reconcileTransactions(ReconciliationRequestDTO request) {
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

        if (isExactMatch(linix, banco)) {
          results.add(buildResult(linix, banco, true, "Coincidencia exacta"));
          matchedIndexes.add(i);
          matched = true;
          break;
        } else if (isFlexibleMatch(linix, banco)) {
          results.add(buildResult(linix, banco, true, "Coincidencia aproximada (fecha o monto)"));
          matchedIndexes.add(i);
          matched = true;
          break;
        }
      }

      if (!matched) {
        results.add(buildResult(linix, null, false, classifyDiscrepancy(linix, "LINIX")));
      }
    }

    for (int i = 0; i < bancoTxs.size(); i++) {
      if (!matchedIndexes.contains(i)) {
        Transaction banco = bancoTxs.get(i);
        results.add(buildResult(null, banco, false, classifyDiscrepancy(banco, "BANCOLOMBIA")));
      }
    }

    log.info("Conciliation completed. Total results: {}", results.size());

    ReconciliationSummaryDTO summary = generateSummary(linixTxs, bancoTxs, results);

    return ReconciliationResponseDTO.builder()
        .results(results)
        .summary(summary)
        .build();
  }

  private ReconciliationSummaryDTO generateSummary(List<Transaction> linixTxs, List<Transaction> bancoTxs,
      List<ConciliationResultDTO> results) {
    int matched = 0;
    int unmatchedLinix = 0;
    int unmatchedBanco = 0;

    for (ConciliationResultDTO result : results) {
      if (result.isMatched())
        matched++;
      else if (result.getLinixTransaction() != null)
        unmatchedLinix++;
      else if (result.getBancolombiaTransaction() != null)
        unmatchedBanco++;
    }

    BigDecimal linixDebits = sumByType(linixTxs, "DEBIT");
    BigDecimal linixCredits = sumByType(linixTxs, "CREDIT");

    BigDecimal bancoDebits = sumByType(bancoTxs, "DEBIT");
    BigDecimal bancoCredits = sumByType(bancoTxs, "CREDIT");

    BigDecimal saldoEstimado = lastBancolombiaStatement.getStartingBalance()
        .add(bancoCredits)
        .subtract(bancoDebits);

    return ReconciliationSummaryDTO.builder()
        .totalLinix(linixTxs.size())
        .totalBancolombia(bancoTxs.size())
        .matchedCount(matched)
        .unmatchedLinix(unmatchedLinix)
        .unmatchedBancolombia(unmatchedBanco)
        .totalLinixAmount(linixDebits.add(linixCredits))
        .totalBancolombiaAmount(bancoDebits.add(bancoCredits))
        .linixDebits(linixDebits)
        .linixCredits(linixCredits)
        .bancolombiaDebits(bancoDebits)
        .bancolombiaCredits(bancoCredits)
        .saldoInicialBanco(lastBancolombiaStatement.getStartingBalance())
        .saldoFinalBanco(lastBancolombiaStatement.getEndingBalance())
        .saldoFinalCalculado(saldoEstimado)
        .diferenciaSaldoFinal(lastBancolombiaStatement.getEndingBalance().subtract(saldoEstimado))
        .build();
  }

  private BigDecimal sumByType(List<Transaction> txs, String type) {
    return txs.stream()
        .filter(tx -> tx.getTransactionType().name().equalsIgnoreCase(type))
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private ConciliationResultDTO buildResult(Transaction linix, Transaction banco, boolean matched, String type) {
    return ConciliationResultDTO.builder()
        .linixTransaction(linix)
        .bancolombiaTransaction(banco)
        .matched(matched)
        .discrepancyType(type)
        .build();
  }

  private boolean isExactMatch(Transaction a, Transaction b) {
    return a.getAmount().compareTo(b.getAmount()) == 0 && a.getDate().isEqual(b.getDate());
  }

  private boolean isFlexibleMatch(Transaction a, Transaction b) {
    BigDecimal tolerance = new BigDecimal("500");
    boolean amountClose = a.getAmount().subtract(b.getAmount()).abs().compareTo(tolerance) <= 0;
    boolean dateClose = !a.getDate().isBefore(b.getDate().minusDays(1))
        && !a.getDate().isAfter(b.getDate().plusDays(1));
    return amountClose && dateClose;
  }

  private String classifyDiscrepancy(Transaction tx, String source) {
    String d = tx.getDescription() != null ? tx.getDescription().toLowerCase() : "";

    if (d.contains("comisión") || d.contains("mantenimiento"))
      return "Comisión bancaria";
    if (d.contains("interés") || d.contains("rendimiento"))
      return "Intereses bancarios";
    if (d.contains("cheque") || d.contains("pendiente"))
      return "Cheque pendiente de cobro";
    if (d.contains("ajuste") || d.contains("error"))
      return "Error de digitación";

    return "BANCOLOMBIA".equals(source)
        ? "Movimiento bancario no registrado en contabilidad"
        : "Movimiento contable no reflejado en banco";
  }
}
