package ru.mpstat.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSegmentationResponse {
  private String range;
  private int revenue;
  @JsonProperty("lost_profit_percent")
  private double lostProfitPercent;
}
