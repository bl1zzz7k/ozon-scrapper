package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowDeliveryService {
  private double total;
  private List<CashFlowDetailServiceItem> items;
}
