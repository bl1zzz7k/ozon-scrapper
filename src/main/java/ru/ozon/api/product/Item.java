package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("product_id")
    private long productID;
    @JsonProperty("offer_id")
    private String offerID;
    private Commissions commissions;
    @JsonProperty("marketing_actions")
    private MarketingActions marketingActions;
    @JsonProperty("volume_weight")
    private double volumeWeight;
    @JsonProperty("price_indexes")
    private PriceIndexes priceIndexes;
    private int acquiring;
    private Price price;
}
