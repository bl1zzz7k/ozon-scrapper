package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlow {
  @JsonProperty("orders_amount")
  private double ordersAmount;
  @JsonProperty("returns_amount")
  private double returnsAmount;
  @JsonProperty("commission_amount")
  private double commissionAmount;
  @JsonProperty("services_amount")
  private double servicesAmount;
  @JsonProperty("item_delivery_and_return_amount")
  private double itemDeliveryAndReturnAmount;
}
