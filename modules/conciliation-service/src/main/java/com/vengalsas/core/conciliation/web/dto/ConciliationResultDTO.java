package com.vengalsas.core.conciliation.web.dto;

import com.vengalsas.core.conciliation.domain.model.Transaction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConciliationResultDTO {

  private Transaction linixTransaction;
  private Transaction bancolombiaTransaction;
  private boolean matched;
}
