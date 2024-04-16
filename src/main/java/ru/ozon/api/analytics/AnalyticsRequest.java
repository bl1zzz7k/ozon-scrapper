package ru.ozon.api.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AnalyticsRequest {
  @JsonProperty("date_from")
  @Builder.Default
  private String dateFrom = LocalDate.now()
      .minusDays(1)
      .format(DateTimeFormatter.ISO_LOCAL_DATE);
  @JsonProperty("date_to")
  @Builder.Default
  private String dateTo = LocalDate.now()
      .minusDays(1)
      .format(DateTimeFormatter.ISO_LOCAL_DATE);
  @Builder.Default
  private List<String> metrics = Lists.newArrayList(
      "revenue",
      "ordered_units",
      "session_view_pdp",
      "hits_view",
      "position_category",
      "conv_tocart",
      "session_view");
  @Builder.Default
  private List<String> dimension = Lists.newArrayList("sku");
  @Builder.Default
  private int limit = 1000;
  @Builder.Default
  private int offset = 0;
}
