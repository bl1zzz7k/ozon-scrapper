package ru.mpstat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterModel {
    private Filter revenue;
    @JsonProperty("final_price")
    private Filter price;
}
