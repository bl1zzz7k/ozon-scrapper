package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockResult {
    private List<StockResultRow> rows = new ArrayList<>();
}
