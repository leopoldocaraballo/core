package com.vengalsas.core.conciliation.web.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReconciliationSummaryDTO {
  private int totalLinix;
  private int totalBancolombia;
  private int matchedCount;
  private int unmatchedLinix;
  private int unmatchedBancolombia;
  private BigDecimal totalLinixAmount;
  private BigDecimal totalBancolombiaAmount;
  private BigDecimal differenceAmount;
}
