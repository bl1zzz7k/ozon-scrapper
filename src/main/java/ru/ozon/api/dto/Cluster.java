package ru.ozon.api.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Cluster {
  private final String name;
  private final int columnIndex;
  private List<String> warehouses = new ArrayList<>();

  public void addWarehouse(String warehouseName) {
    warehouses.add(warehouseName);
  }
}
