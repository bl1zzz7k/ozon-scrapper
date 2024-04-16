package ru.ozon.api.analytics;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
  @JsonProperty("operation_type")
  private String operationType;
  @JsonProperty("operation_date")
  private String operationDate;
  @JsonProperty("operation_type_name")
  private String operationTypeName;
  private String type;
  @JsonProperty("delivery_charge")
  private double deliveryCharge;
  @JsonProperty("return_delivery_charge")
  private double returnDeliveryCharge;
  @JsonProperty("accruals_for_sale")
  private double accrualsForSale;
  @JsonProperty("sale_commission")
  private double saleCommission;
  private double amount;
  private List<TransactionService> services;
}
