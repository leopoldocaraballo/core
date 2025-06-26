package com.vengalsas.core.conciliation.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that encapsulates the full reconciliation response, including
 * matched/unmatched results and a summary of totals and differences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponseDTO {

  private List<ConciliationResultDTO> results;

  private ReconciliationSummaryDTO summary;
}
