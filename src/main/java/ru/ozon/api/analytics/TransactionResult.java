package ru.ozon.api.analytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResult {
  private List<Transaction> operations;
  @JsonProperty("page_count")
  private int pageCount;
  @JsonProperty("row_count")
  private int rowCount;
}
