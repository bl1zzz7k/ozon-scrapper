package ru.ozon.api.finance.realization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealizationResultHeader {
  @JsonProperty("doc_amount")
  private double docAmount;
  @JsonProperty("vat_amount")
  private double vatAmount;
}
