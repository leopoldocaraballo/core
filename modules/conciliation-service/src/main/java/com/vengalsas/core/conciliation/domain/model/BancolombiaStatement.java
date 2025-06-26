package com.vengalsas.core.conciliation.domain.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BancolombiaStatement {
  private List<Transaction> transactions;
  private BigDecimal startingBalance;
  private BigDecimal endingBalance;
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
}
