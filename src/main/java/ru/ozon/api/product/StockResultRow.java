package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockResultRow {
    @JsonProperty("item_code")
    private String itemCode;
    @JsonProperty("free_to_sell_amount")
    private int freeToSellAmount;
    @JsonProperty("reserved_amount")
    private int reservedAmount;
}
