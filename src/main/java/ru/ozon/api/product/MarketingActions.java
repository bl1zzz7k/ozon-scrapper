package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketingActions {
    @JsonProperty("current_period_from")
    private OffsetDateTime currentPeriodFrom;
    @JsonProperty("current_period_to")
    private OffsetDateTime currentPeriodTo;
    private List<Action> actions;
    @JsonProperty("ozon_actions_exist")
    private boolean ozonActionsExist;
}
