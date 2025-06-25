package com.vengalsas.core.conciliation.web.dto;

import java.util.List;

import com.vengalsas.core.conciliation.domain.model.Transaction;

import lombok.Data;

@Data
public class ReconciliationRequestDTO {
  private List<Transaction> linixTransactions;
  private List<Transaction> bancolombiaTransactions;
}
