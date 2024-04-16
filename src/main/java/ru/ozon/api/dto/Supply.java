package ru.ozon.api.dto;

import java.util.List;
import lombok.Data;

@Data
public class Supply {
  private String warehouse;
  private List<SupplyItem> supplyItems;
}
