package ru.ozon.api.analytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsData {
  private List<Dimension> dimensions;
  private List<Number> metrics;
}
