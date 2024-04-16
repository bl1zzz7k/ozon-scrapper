package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {
    @JsonProperty("date_from")
    private OffsetDateTime dateFrom;
    @JsonProperty("date_to")
    private OffsetDateTime dateTo;
    private String title;
    @JsonProperty("discount_value")
    private double discountValue;
}
