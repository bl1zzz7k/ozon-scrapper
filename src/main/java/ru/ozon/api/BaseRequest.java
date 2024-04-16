package ru.ozon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BaseRequest {
  private BaseFilter filter;
  @Builder.Default
  private int page = 1;
  @Builder.Default
  @JsonProperty("page_size")
  private int pageSize = 1000;
}
