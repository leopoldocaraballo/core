package com.vengalsas.core.conciliation.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vengalsas.core.conciliation.domain.model.SourceSystem;
import com.vengalsas.core.conciliation.domain.model.TransactionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
  private LocalDate date;
  private String description;
  private BigDecimal amount;
  private TransactionType transactionType;
  private SourceSystem source;
}
