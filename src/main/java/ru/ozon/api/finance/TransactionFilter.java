package ru.ozon.api.finance;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ru.ozon.api.BaseFilter;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class TransactionFilter extends BaseFilter {
  @Builder.Default
  @JsonProperty("operation_type")
  private List<String> operationType = new ArrayList<>();
  @Builder.Default
  @JsonProperty("posting_number")
  private String postingNumber = "";
  @Builder.Default
  @JsonProperty("transaction_type")
  private String transactionType = "all";
}
