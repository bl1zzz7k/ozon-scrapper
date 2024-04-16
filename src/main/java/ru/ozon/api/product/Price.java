package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Price {
    private String price;
    @JsonProperty("old_price")
    private String oldPrice;
    @JsonProperty("premium_price")
    private String premiumPrice;
    @JsonProperty("recommended_price")
    private String recommendedPrice;
    @JsonProperty("retail_price")
    private String retailPrice;
    private String vat;
    @JsonProperty("min_ozon_price")
    private String minOzonPrice;
    @JsonProperty("marketing_price")
    private String marketingPrice;
    @JsonProperty("marketing_seller_price")
    private String marketingSellerPrice;
    @JsonProperty("min_price")
    private String minPrice;
    @JsonProperty("currency_code")
    private String currencyCode;
    @JsonProperty("auto_action_enabled")
    private boolean autoActionEnabled;
}
