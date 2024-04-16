package ru.mpstat.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SellersResponse {
  private String name;
  private int revenue;
  private int sales;
  private int position;
}
