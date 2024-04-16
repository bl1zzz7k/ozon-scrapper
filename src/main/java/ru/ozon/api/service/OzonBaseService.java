package ru.ozon.api.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;

public abstract class OzonBaseService extends SeleniumBaseService {
  public static final String OZON_BASE_URL = "https://seller.ozon.ru/app";
  public static final String OZON_API_BASE_URL = "https://api-seller.ozon.ru";
  public static final String OZON_API_KEY = "";
  public static final String OZON_API_CLIENT_ID = "";
  public static final int AWAIT_ELEMENT_TIME_SEC = 50;
  public static final int AWAIT_DOWNLOAD_TIME_SEC = 30;
  public final ObjectMapper objectMapper = new ObjectMapper();

  public final Map<String, String> skuMap = Map.of(
      "1261583244", "lenta_m"
  );

  protected OzonBaseService() {
    super();
    objectMapper.registerModule(new JavaTimeModule());
  }
}
