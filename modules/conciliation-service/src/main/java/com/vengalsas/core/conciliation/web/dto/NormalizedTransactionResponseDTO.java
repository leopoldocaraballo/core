package com.vengalsas.core.conciliation.web.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NormalizedTransactionResponseDTO {
  private List<TransactionResponseDTO> linixTransactions;
  private List<TransactionResponseDTO> bancolombiaTransactions;
}
