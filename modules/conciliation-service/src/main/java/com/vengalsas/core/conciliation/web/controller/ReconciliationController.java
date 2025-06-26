package com.vengalsas.core.conciliation.web.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.vengalsas.core.conciliation.application.service.ReconciliationService;
import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.Transaction;
import com.vengalsas.core.conciliation.web.dto.NormalizedTransactionResponseDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationRequestDTO;
import com.vengalsas.core.conciliation.web.dto.ReconciliationResponseDTO;
import com.vengalsas.core.conciliation.web.dto.TransactionResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Operation(summary = "Upload bank and accounting files", description = "Reads Bancolombia (.xlsx) and Linix (.csv or .txt) files, extracts transactions, and returns them normalized.", responses = {
      @ApiResponse(responseCode = "200", description = "Normalized transactions returned", content = @Content(schema = @Schema(implementation = NormalizedTransactionResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid or missing files", content = @Content)
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<NormalizedTransactionResponseDTO> uploadFiles(
      @RequestPart("bankFile") MultipartFile bankFile,
      @RequestPart("accountingFile") MultipartFile accountingFile) {

    if (bankFile == null || bankFile.isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "Bank file is missing or empty.");
    }

    if (accountingFile == null || accountingFile.isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "Accounting file is missing or empty.");
    }

    logger.info("Received bankFile: {} ({} bytes)", bankFile.getOriginalFilename(), bankFile.getSize());
    logger.info("Received accountingFile: {} ({} bytes)", accountingFile.getOriginalFilename(),
        accountingFile.getSize());

    try {
      List<Transaction> transactions = reconciliationService.readAndNormalize(bankFile, accountingFile);

      List<TransactionResponseDTO> linixTransactions = transactions.stream()
          .filter(tx -> tx.getSource() == SourceSystem.LINIX)
          .map(this::toDto)
          .collect(Collectors.toList());

      List<TransactionResponseDTO> bancolombiaTransactions = transactions.stream()
          .filter(tx -> tx.getSource() == SourceSystem.BANCOLOMBIA)
          .map(this::toDto)
          .collect(Collectors.toList());

      if (linixTransactions.isEmpty()) {
        logger.warn("No Linix transactions found.");
      }

      if (bancolombiaTransactions.isEmpty()) {
        logger.warn("No Bancolombia transactions found.");
      }

      logger.info("Normalized: {} Linix, {} Bancolombia", linixTransactions.size(), bancolombiaTransactions.size());

      return ResponseEntity.ok(NormalizedTransactionResponseDTO.builder()
          .linixTransactions(linixTransactions)
          .bancolombiaTransactions(bancolombiaTransactions)
          .build());

    } catch (Exception e) {
      logger.error("Error while reading files: {}", e.getMessage(), e);
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error while processing files.");
    }
  }

  @Operation(summary = "Reconcile transactions", description = "Receives lists of Linix and Bancolombia transactions and returns matched/unmatched results with a summary.", responses = {
      @ApiResponse(responseCode = "200", description = "Reconciliation results returned", content = @Content(schema = @Schema(implementation = ReconciliationResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
  })
  @PostMapping(value = "/reconcile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ReconciliationResponseDTO> reconcile(@RequestBody ReconciliationRequestDTO request) {
    if (request == null ||
        request.getLinixTransactions() == null || request.getLinixTransactions().isEmpty() ||
        request.getBancolombiaTransactions() == null || request.getBancolombiaTransactions().isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "Reconciliation request is invalid or incomplete.");
    }

    logger.info("Reconciling {} Linix and {} Bancolombia transactions",
        request.getLinixTransactions().size(), request.getBancolombiaTransactions().size());

    try {
      ReconciliationResponseDTO response = reconciliationService.reconcileTransactions(request);
      logger.info("Reconciliation completed: {} results", response.getResults().size());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Reconciliation error: {}", e.getMessage(), e);
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error during reconciliation process.");
    }
  }

  private TransactionResponseDTO toDto(Transaction tx) {
    return TransactionResponseDTO.builder()
        .date(tx.getDate())
        .description(tx.getDescription())
        .amount(tx.getAmount())
        .transactionType(tx.getTransactionType())
        .source(tx.getSource())
        .build();
  }
}
