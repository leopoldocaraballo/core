package com.vengalsas.core.conciliation.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponseDTO {
  private List<ConciliationResultDTO> results;
  private ReconciliationSummaryDTO summary;
}
