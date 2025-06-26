package com.vengalsas.core.conciliation.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDTO {

  @Schema(example = "2025-04-01", description = "Transaction date in ISO format")
  private LocalDate date;

  @Schema(example = "Pago factura Facebook", description = "Transaction description")
  private String description;

  @Schema(example = "150000.00", description = "Positive amount of the transaction")
  private BigDecimal amount;

  @Schema(example = "DEBIT", description = "Transaction type: DEBIT or CREDIT")
  private TransactionType transactionType;

  @Schema(example = "LINIX", description = "Source system identifier")
  private SourceSystem source;

  @Schema(example = "1000470", description = "Optional reference or document number")
  private String reference;

}