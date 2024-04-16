package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceIndexes {
    @JsonProperty("price_index")
    private String priceIndex;
    @JsonProperty("external_index_data")
    private IndexData externalIndexData;
    @JsonProperty("ozon_index_data")
    private IndexData ozonIndexData;
    @JsonProperty("self_marketplaces_index_data")
    private IndexData selfMarketplacesIndexData;
}
