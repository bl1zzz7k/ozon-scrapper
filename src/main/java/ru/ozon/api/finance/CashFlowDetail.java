package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowDetail {
  private List<Payment> payments;
  private CashFlowDetailItem delivery;
  @JsonProperty("return")
  private CashFlowDetailItem returnService;
  private CashFlowDeliveryService services;
  private CashFlowDeliveryService others;
}
