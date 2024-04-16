package ru.ozon.api.service;

import static ru.ozon.api.Utils.getFilesByName;
import static ru.ozon.api.Utils.waitUntilFileWillBeDownloaded;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.ozon.api.analytics.AnalyticsRequest;
import ru.ozon.api.analytics.AnalyticsResult;
import ru.ozon.api.dto.Metrics;
import ru.ozon.api.dto.SkuItem;
import ru.ozon.api.product.Action;
import ru.ozon.api.product.BaseResponse;
import ru.ozon.api.product.MarketingActions;
import ru.ozon.api.product.PricesResult;
import ru.ozon.api.product.StockResult;
import ru.ozon.api.product.StockResultRow;
import ru.ozon.api.reports.AnalyticItem;
import ru.ozon.api.reports.Analytics;


@Log4j2
@RequiredArgsConstructor
@Command(name = "update-statistic", description = "Update analytics data")
public class OzonSkuAnalyticsService extends OzonBaseService {
  private static final String EXPENSES_FILENAME = "campaign_expense";
  private static final String EXPENSES_PP_FILENAME = "search_promo_organisation_products_report";
  private final String SPREADSHEET_ID = "1QssLsXfYxkbXWJHTRNVRQA3VgfC_wP8NZczh--M6dHA";
  private final Integer SHEET_ID = 1373871606;


  @Override
  public void process() throws Exception {
    getFilesByName(EXPENSES_FILENAME).forEach(File::delete);
    getFilesByName(EXPENSES_PP_FILENAME).forEach(File::delete);

    Metrics metrics = new Metrics();
    double logisticCoef = getLogisticCoef();

    metrics.setLogisticCoefficient(logisticCoef);

    getDrrReports();
    fillDrrMetricsExcells(metrics);
    fillPricesAndCommissions(metrics);
    fillStocks(metrics);

    GoogleSheetsService googleSheetsService = new GoogleSheetsService(SPREADSHEET_ID);
    googleSheetsService.updatePricesSheet(metrics, SHEET_ID);

    log.info(new ObjectMapper().writeValueAsString(metrics));
  }

  private void fillStocks(Metrics metrics) {
    final HttpPost httpPost = createHttpPostForStocks();
    StockResult result =
        Optional.ofNullable(getApiResult(httpPost, new TypeReference<BaseResponse<StockResult>>() {
        })).orElseGet(StockResult::new);
    Map<String, StockResultRow> rowsMap = new HashMap<>();
    result.getRows().forEach(row -> Optional.ofNullable(rowsMap.get(row.getItemCode()))
        .ifPresentOrElse(stockResultRow -> {
              stockResultRow.setFreeToSellAmount(
                  stockResultRow.getFreeToSellAmount() + row.getFreeToSellAmount());
              stockResultRow.setReservedAmount(
                  stockResultRow.getReservedAmount() + row.getReservedAmount());
            },
            () ->
                rowsMap.put(row.getItemCode(), row)
        ));
    rowsMap.values().forEach(row ->
        metrics.getSkuItems().stream()
            .filter(skuItem -> skuItem.getSkuName().equalsIgnoreCase(row.getItemCode())).findAny()
            .ifPresentOrElse(metric ->
                    metric.setStock(row.getFreeToSellAmount() + row.getReservedAmount()),
                () -> metrics.addSkuItem(SkuItem.builder()
                    .skuName(row.getItemCode())
                    .stock(row.getFreeToSellAmount() + row.getReservedAmount())
                    .build()))
    );
  }

  private void getDrrReports() {
    driver.get(OZON_BASE_URL + "/advertisement/!#/budget-details");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AWAIT_ELEMENT_TIME_SEC));

    WebElement filterElement = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//div[contains(@class, 'UiDateRangeInput')]")));
    filterElement.click();
    ZonedDateTime yesterday = ZonedDateTime.now().minusDays(1);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EE LLL dd yyyy 00:00:00 'GMT'Z");
    String formattedString = yesterday.format(formatter);

    List<WebElement> yesterdayDates =
        driver.findElements(By.xpath("//*[@momentdate='" + formattedString + "']"));
    if (yesterdayDates.isEmpty()) {
      throw new IllegalArgumentException(
          "Element //*[@momentdate='" + formattedString + "'] doesn't found");
    }
    yesterdayDates.forEach(WebElement::click);

    WebElement applyButton = driver.findElement(By.xpath("//*[ contains (text(), 'Применить' ) ]"));
    applyButton.click();

    boolean dataIsNotEmpty = driver.findElements(
        By.xpath("//div[contains (text(), 'Нет расходов за выбранный период')]")).isEmpty();

    if (dataIsNotEmpty) {
      WebElement downloadExcelButton = wait.until(ExpectedConditions.elementToBeClickable(
          By.xpath("//*[ contains (text(), 'Скачать в Excel')]")));
      downloadExcelButton.click();
      waitUntilFileWillBeDownloaded(AWAIT_DOWNLOAD_TIME_SEC, EXPENSES_FILENAME);
    } else {
      log.error("DRR expenses is empty");
    }
  }

  @SneakyThrows
  private void fillPricesAndCommissions(Metrics metrics) {
    final HttpPost httpPost = createHttpPostForPricesAndCommissions();
    PricesResult result =
        Optional.ofNullable(getApiResult(httpPost, new TypeReference<BaseResponse<PricesResult>>() {
        })).orElseGet(PricesResult::new);
    result.getItems().forEach(item -> {
          double logisticPrice = item.getCommissions().getFboReturnFlowTransMinAmount();
          double lastMilePrice = item.getCommissions().getFboDelivToCustomerAmount();
          int acquiring = item.getAcquiring();
          double salesCommission = item.getCommissions().getSalesPercentFbo() / 100;
          double skuPrice = item.getPrice().getMarketingPrice().isEmpty() ? 0d :
              Double.parseDouble(item.getPrice().getMarketingPrice());
          double skuMyPrice = Optional.ofNullable(item.getMarketingActions()).map(
                  MarketingActions::getActions).stream()
              .flatMap(Collection::stream)
              .filter(action -> action.getTitle().equalsIgnoreCase("хоспи"))
              .findAny().map(Action::getDiscountValue).orElse(0d);
          String priceIndex = item.getPriceIndexes().getPriceIndex();

          metrics.getSkuItems().stream()
              .filter(skuItem -> skuItem.getSkuName().equalsIgnoreCase(item.getOfferID())).findAny()
              .ifPresentOrElse(metric -> {
                metric.setLogisticPrice(logisticPrice);
                metric.setLastMilePrice(lastMilePrice);
                metric.setAcquiring(acquiring);
                metric.setSalesCommission(salesCommission);
                metric.setSkuPrice(skuPrice);
                metric.setSkuMyPrice(skuMyPrice);
                metric.setPriceIndex(priceIndex);
              }, () -> metrics.addSkuItem(SkuItem.builder()
                  .skuName(item.getOfferID())
                  .logisticPrice(logisticPrice)
                  .lastMilePrice(lastMilePrice)
                  .acquiring(acquiring)
                  .salesCommission(salesCommission)
                  .skuPrice(skuMyPrice)
                  .skuMyPrice(skuMyPrice)
                  .priceIndex(priceIndex)
                  .build()));
        }
    );
  }

  private void fillDrrMetricsExcells(Metrics metrics) {
    List<File> expensesFiles = getFilesByName(EXPENSES_FILENAME);
    if (expensesFiles.size() > 1) {
      throw new IllegalArgumentException("expensesFile size=" + expensesFiles.size());
    } else if (expensesFiles.isEmpty()) {
      log.warn("{} doesn't exists", EXPENSES_FILENAME);
    }

    Analytics analytics = getAnalytics();

    if (!expensesFiles.isEmpty()) {
      parseExpensesFile(expensesFiles.get(0), analytics.getAnalyticItems());
    }
    log.debug("SumSalesTotalAmount: {}, SumExpensesDrrAmount: {}. {}",
        analytics.getSumSalesTotalAmount(), analytics.getSumExpensesDrrAmount(), analytics);

    getPPAnalytics(analytics.getAnalyticItems());

    analytics.getAnalyticItems().stream()
        .filter(analyticItem -> analyticItem.getRevenue().compareTo(BigDecimal.ZERO) > 0)
        .forEach(analyticItem -> {
          double drr = analyticItem.getExpensesDrrAmount().multiply(new BigDecimal(100))
              .divide(analyticItem.getRevenue(), RoundingMode.HALF_DOWN).doubleValue();

          double conversionInSales =
              (double) analyticItem.getOrderedUnits() / analyticItem.getUniqueUser();

          double uctr = (double)
              analyticItem.getClicks() / analyticItem.getUniqueUser();

          metrics.getSkuItems().stream()
              .filter(skuItem -> skuItem.getSkuName().equalsIgnoreCase(analyticItem.getSkuName()))
              .findAny()
              .ifPresentOrElse(skuItem -> {
                skuItem.setDrr(drr);
                skuItem.setSales(analyticItem.getOrderedUnits());
                skuItem.setConversionInCart(analyticItem.getConversionInCart());
                skuItem.setConversionInSales(conversionInSales);
                skuItem.setViews(analyticItem.getViews());
                skuItem.setPosition(analyticItem.getPosition());
                skuItem.setUctr(uctr);
              }, () -> metrics.addSkuItem(SkuItem.builder()
                  .skuName(analyticItem.getSkuName())
                  .drr(drr)
                  .sales(analyticItem.getOrderedUnits())
                  .conversionInCart(analyticItem.getConversionInCart())
                  .conversionInSales(conversionInSales)
                  .views(analyticItem.getViews())
                  .position(analyticItem.getPosition())
                  .uctr(uctr)
                  .build()));
        });
  }

  private void getPPAnalytics(List<AnalyticItem> analyticItems) {
    File ppExpenses = downloadPPExpenses();
    try (
        CSVParser ppExpensesParser = new CSVParser(new FileReader(ppExpenses),
            CSVFormat.Builder.create()
                .setDelimiter(";")
                .setSkipHeaderRecord(true)
                .build())) {
      List<CSVRecord> records = ppExpensesParser.getRecords();
      analyticItems.forEach(analyticItem -> records.forEach(record -> {
        String skuName = record.get(1);
        if (skuName.equalsIgnoreCase(analyticItem.getSkuName())) {
          BigDecimal drrExpenses = new BigDecimal(record.get(10).replace(",", "."));
          analyticItem.setExpensesDrrAmount(analyticItem.getExpensesDrrAmount().add(drrExpenses));
        }
      }));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private File downloadPPExpenses() {
    driver.get(OZON_BASE_URL + "/advertisement/!#/product/search");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AWAIT_ELEMENT_TIME_SEC));

    WebElement additionalButton = wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath("//span[ contains (text(), 'Дополнительно')]"),
            1)).get(0);

    additionalButton.click();

    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath("//div[ contains (text(), 'Показатели по товарам')]"),
            1)).get(0).click();


    WebElement dateInput = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.xpath("//input[@placeholder = 'дд.мм.гггг – дд.мм.гггг']")
            ));

    LocalDate yesterday = LocalDate.now().minusDays(1);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy – dd.MM.yyyy");
    String formattedDate = yesterday.format(formatter);

    dateInput.sendKeys(formattedDate);

    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath("//span[ contains (text(), 'Скачать CSV')]"),
            1)).get(0).click();

    wait.until(ExpectedConditions.numberOfElementsToBe(
                By.xpath("//span[ contains (text(), 'Дополнительно')]/../../../../../../div"), 2));

    wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//span[ contains (text(), 'Дополнительно')]/../../../../../../div[1]/div/div/button")))
        .click();

    wait.until(ExpectedConditions.numberOfElementsToBe(
            By.xpath("//td[contains (text(), '" + formattedDate + "')][1]/../../tr[1]/td[1]/div/div/*[contains(@class,'okIcon')]"),
            1));

    wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//td[contains (text(), '" + formattedDate + "')][1]/../../tr[1]/td[5]")))
        .click();

    List<File> result =
        waitUntilFileWillBeDownloaded(AWAIT_DOWNLOAD_TIME_SEC, EXPENSES_PP_FILENAME);
    if (result.size() != 1) {
      throw new IllegalStateException("file size " + EXPENSES_PP_FILENAME + ": " + result.size());
    }
    return result.get(0);
  }

  @SneakyThrows
  private void parseExpensesFile(File expensesFile, List<AnalyticItem> analyticItems) {
    try (FileInputStream file = new FileInputStream(expensesFile);
         Workbook workbook = new XSSFWorkbook(file)) {
      Sheet sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        if (row.getRowNum() > 1) {
          String skuName = row.getCell(1).toString().split("\\|")[0];
          BigDecimal expensesDrrAmount = new BigDecimal(row.getCell(4).toString());
          analyticItems.stream()
              .filter(expensesItem -> expensesItem.getSkuName().equalsIgnoreCase(skuName)).findAny()
              .ifPresentOrElse(expensesItem -> expensesItem.setExpensesDrrAmount(
                      expensesItem.getExpensesDrrAmount().add(expensesDrrAmount)),
                  () -> analyticItems.add(
                      AnalyticItem.builder()
                          .skuName(skuName)
                          .expensesDrrAmount(expensesDrrAmount)
                          .build()));
        }
      }
    }
  }

  private Analytics getAnalytics() {
    final HttpPost httpPost = createHttpPostForAnalytics();

    AnalyticsResult result =
        Optional.ofNullable(
            getApiResult(httpPost, new TypeReference<BaseResponse<AnalyticsResult>>() {
            })).orElseGet(AnalyticsResult::new);

    List<AnalyticItem> analyticItems = result.getData().stream().map(analyticsData -> {
          String skuName = skuMap.get(analyticsData.getDimensions().get(0).getId());
          if (skuName == null) {
            return null;
          } else {
            List<Number> metrics = analyticsData.getMetrics();
            return AnalyticItem.builder()
                .skuName(skuName)
                .revenue(new BigDecimal(metrics.get(0).intValue()))
                .orderedUnits(metrics.get(1).intValue())
                .clicks(metrics.get(2).intValue())
                .views(metrics.get(3).intValue())
                .position(metrics.get(4).intValue())
                .conversionInCart(metrics.get(5).doubleValue())
                .uniqueUser(metrics.get(6).intValue())
                .build();
          }
        }).filter(Objects::nonNull)
        .collect(Collectors.toList());
    return new Analytics(analyticItems);
  }


  @SneakyThrows
  private <T> T getApiResult(HttpPost httpPost, TypeReference<BaseResponse<T>> typeReference) {
    T result = null;
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      String response = client.execute(httpPost, new BasicResponseHandler());
      result = objectMapper.readValue(response, typeReference).getResult();
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return result;
  }

  @SneakyThrows
  private HttpPost createHttpPostForAnalytics() {
    String analyticsDataPath = "/v1/analytics/data";

    final HttpPost httpPost = createHttpPost(analyticsDataPath);

    final String json = objectMapper.writeValueAsString(AnalyticsRequest.builder().build());
    final StringEntity entity = new StringEntity(json);
    httpPost.setEntity(entity);
    return httpPost;
  }

  private HttpPost createHttpPostForPricesAndCommissions() throws UnsupportedEncodingException {
    String productPricesInfoPath = "/v4/product/info/prices";

    final HttpPost httpPost = createHttpPost(productPricesInfoPath);

    final String json =
        "{ \"filter\": { \"offer_id\": [],\"visibility\": \"ALL\"}, \"limit\": 100}";
    final StringEntity entity = new StringEntity(json);
    httpPost.setEntity(entity);
    return httpPost;
  }

  @SneakyThrows
  private HttpPost createHttpPostForStocks() {
    String stocksPath = "/v2/analytics/stock_on_warehouses";

    final HttpPost httpPost = createHttpPost(stocksPath);

    final String json = "{\"limit\": 1000, \"offset\": 0, \"warehouse_type\": \"ALL\"}";

    final StringEntity entity = new StringEntity(json);
    httpPost.setEntity(entity);
    return httpPost;
  }

  private HttpPost createHttpPost(String productPricesInfoPath) {
    final HttpPost httpPost = new HttpPost(OZON_API_BASE_URL + productPricesInfoPath);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("Client-Id", OZON_API_CLIENT_ID);
    httpPost.setHeader("Api-Key", OZON_API_KEY);
    return httpPost;
  }

  private double getLogisticCoef() {
    driver.get(OZON_BASE_URL + "/dashboard/main");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AWAIT_ELEMENT_TIME_SEC));

    WebElement logisticIndexElement = wait.until(ExpectedConditions.presenceOfElementLocated(
        By.xpath("//div[contains(@class, 'styles_currentValue')]")));
    int logisticSalePercent = Integer.parseInt(logisticIndexElement.getText().split("%")[0]);
    double logisticCoefficient = getLogisticCoefficient(logisticSalePercent);
    if (logisticCoefficient >= 0.85) {
      log.warn("logisticCoefficient = {}", logisticCoefficient);
    }
    return logisticCoefficient;
  }

     /*
        https://seller-edu.ozon.ru/commissions-tariffs/legal-information/full-actual-commissions#2-1-1-%D0%BB%D0%BE%D0%B3%D0%B8%D1%81%D1%82%D0%B8%D0%BA%D0%B0
     */

  private static double getLogisticCoefficient(int logisticSalePercent) {
    if (logisticSalePercent >= 0 && logisticSalePercent <= 59) {
      return 1.2;
    } else if (logisticSalePercent >= 60 && logisticSalePercent <= 64) {
      return 1.1;
    } else if (logisticSalePercent >= 65 && logisticSalePercent <= 74) {
      return 1;
    } else if (logisticSalePercent >= 75 && logisticSalePercent <= 79) {
      return 0.95;
    } else if (logisticSalePercent >= 80 && logisticSalePercent <= 84) {
      return 0.9;
    } else if (logisticSalePercent >= 85 && logisticSalePercent <= 89) {
      return 0.85;
    } else if (logisticSalePercent >= 90 && logisticSalePercent <= 94) {
      return 0.80;
    } else if (logisticSalePercent >= 95) {
      return 0.5;
    }
    throw new IllegalArgumentException("logisticSalePercent = " + logisticSalePercent);
  }
}
