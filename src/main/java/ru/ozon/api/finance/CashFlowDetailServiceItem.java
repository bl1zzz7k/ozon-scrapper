package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowDetailServiceItem {
  private String name;
  private double price;
}
