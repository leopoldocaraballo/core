package com.vengalsas.core.conciliation.web.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vengalsas.core.conciliation.application.service.ReconciliationService;
import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.web.dto.ConciliationResultDTO;
import com.vengalsas.core.conciliation.web.dto.NormalizedTransactionResponseDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationRequestDTO;
import com.vengalsas.core.conciliation.web.dto.TransactionResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/conciliation")
@RequiredArgsConstructor
@Tag(name = "Conciliation", description = "Endpoints for reconciling bank and accounting transactions")
public class ReconciliationController {

  private final ReconciliationService reconciliationService;
  private static final Logger logger = LoggerFactory.getLogger(ReconciliationController.class);

  /**
   * Uploads two files and normalizes transactions by source system.
   */
  @Operation(summary = "Upload bank and accounting files", description = "Reads Bancolombia (.xlsx) and Linix (.csv or .txt) files, extracts transactions, and returns them normalized.", responses = {
      @ApiResponse(responseCode = "200", description = "Normalized transactions returned"),
      @ApiResponse(responseCode = "400", description = "Invalid or missing files", content = @Content)
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<NormalizedTransactionResponseDTO> uploadFiles(
      @RequestPart("bankFile") MultipartFile bankFile,
      @RequestPart("accountingFile") MultipartFile accountingFile) throws Exception {

    if (bankFile == null || bankFile.isEmpty()) {
      throw new IllegalArgumentException("Bank file is missing or empty.");
    }
    if (accountingFile == null || accountingFile.isEmpty()) {
      throw new IllegalArgumentException("Accounting file is missing or empty.");
    }

    logger.info("Received bankFile: {}, size={} bytes", bankFile.getOriginalFilename(), bankFile.getSize());
    logger.info("Received accountingFile: {}, size={} bytes", accountingFile.getOriginalFilename(),
        accountingFile.getSize());

    List<Transaction> transactions = reconciliationService.readAndNormalize(bankFile, accountingFile);

    List<TransactionResponseDTO> linixTransactions = transactions.stream()
        .filter(tx -> tx.getSource() == SourceSystem.LINIX)
        .map(this::toDto)
        .collect(Collectors.toList());

    List<TransactionResponseDTO> bancolombiaTransactions = transactions.stream()
        .filter(tx -> tx.getSource() == SourceSystem.BANCOLUMBIA)
        .map(this::toDto)
        .collect(Collectors.toList());

    if (linixTransactions.isEmpty()) {
      logger.warn("No Linix transactions found in file.");
    }
    if (bancolombiaTransactions.isEmpty()) {
      logger.warn("No Bancolombia transactions found in file.");
    }

    logger.info("Parsed {} Linix transactions and {} Bancolombia transactions",
        linixTransactions.size(), bancolombiaTransactions.size());

    return ResponseEntity.ok(NormalizedTransactionResponseDTO.builder()
        .linixTransactions(linixTransactions)
        .bancolombiaTransactions(bancolombiaTransactions)
        .build());
  }

  /**
   * Performs transaction reconciliation between uploaded sources.
   */
  @Operation(summary = "Reconcile transactions", description = "Receives lists of Linix and Bancolombia transactions and returns matched/unmatched results.", responses = {
      @ApiResponse(responseCode = "200", description = "Reconciliation results returned"),
      @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
  })
  @PostMapping(value = "/reconcile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ConciliationResultDTO>> reconcile(@RequestBody ReconciliationRequestDTO request) {
    if (request == null ||
        request.getLinixTransactions() == null || request.getLinixTransactions().isEmpty() ||
        request.getBancolombiaTransactions() == null || request.getBancolombiaTransactions().isEmpty()) {
      throw new IllegalArgumentException("Reconciliation request body is invalid or missing.");
    }

    logger.info("Reconciling {} Linix and {} Bancolombia transactions",
        request.getLinixTransactions().size(), request.getBancolombiaTransactions().size());

    List<ConciliationResultDTO> results = reconciliationService.reconcileTransactions(request);
    return ResponseEntity.ok(results);
  }

  /**
   * Converts a Transaction entity to its DTO representation.
   */
  private TransactionResponseDTO toDto(Transaction tx) {
    return TransactionResponseDTO.builder()
        .date(tx.getDate())
        .description(tx.getDescription())
        .amount(tx.getAmount())
        .transactionType(tx.getTransactionType())
        .source(tx.getSource())
        .reference(tx.getReference())
        .build();
  }
}
