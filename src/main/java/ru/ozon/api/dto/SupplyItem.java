package ru.ozon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupplyItem {
  private final String skuName;
  private Integer amount;

  public void addAmount(int amount) {
    this.amount += amount;
  }
}
