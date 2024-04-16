package ru.ozon.api.finance.realization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealizationResult {
  private RealizationResultHeader header;
  private List<RealizationResultRow> rows;
}
