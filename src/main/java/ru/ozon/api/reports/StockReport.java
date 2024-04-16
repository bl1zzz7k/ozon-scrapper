package ru.ozon.api.reports;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockReport {
  private List<StockReportItem> items;
}
