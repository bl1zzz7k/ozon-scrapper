package ru.ozon.api.finance.realization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealizationResultRow {
  @JsonProperty("product_id")
  private String productId;
  @JsonProperty("product_name")
  private String productName;
  @JsonProperty("offer_id")
  private String offer_Id;
  @JsonProperty("barcode")
  private String barcode;
  @JsonProperty("sale_qty")
  private int saleQty;
  @JsonProperty("sale_amount")
  private double saleAmount;
  @JsonProperty("sale_commission")
  private double saleCommission;
  @JsonProperty("return_price_seller")
  private double returnPriceSeller;
}
