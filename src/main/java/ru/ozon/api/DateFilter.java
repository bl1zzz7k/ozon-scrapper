package ru.ozon.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DateFilter {
  private String from;
  private String to;

}
