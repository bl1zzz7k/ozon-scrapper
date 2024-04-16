package ru.ozon.api.reports;

import lombok.Builder;
import lombok.Data;
import ru.ozon.api.dto.Cluster;

@Data
@Builder
public class StockReportItem {
  private String skuName;
  private String warehouseName;
  private int skuAmount;
  private double idc;
  private Cluster cluster;
}
