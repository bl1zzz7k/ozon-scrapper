package ru.ozon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class CashFlowRequest extends BaseFilter {
  @Builder.Default
  @JsonProperty("with_details")
  private boolean withDetails = true;
  @Builder.Default
  private int page = 1;
  @Builder.Default
  @JsonProperty("page_size")
  private int pageSize = 1000;
}
