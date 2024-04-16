package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowResult {
  @JsonProperty("cash_flows")
  private List<CashFlow> cashFlows;
  private List<CashFlowDetail> details;
}
