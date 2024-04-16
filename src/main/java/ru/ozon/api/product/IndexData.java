package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexData {
    @JsonProperty("minimal_price")
    private String minimalPrice;
    @JsonProperty("minimal_price_currency")
    private String minimalPriceCurrency;
    @JsonProperty("price_index_value")
    private double priceIndexValue;
}
