package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowDetailItem {
  private double total;
  private double amount;
  @JsonAlias({"delivery_services", "return_services"})
  private CashFlowDeliveryService services;
}
