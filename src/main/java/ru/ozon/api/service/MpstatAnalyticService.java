package ru.ozon.api.service;

import static java.lang.Math.max;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.ozon.api.Utils.getFilesByName;
import static ru.ozon.api.Utils.waitUntilFileWillBeDownloaded;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import com.google.common.io.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.mpstat.Category;
import ru.mpstat.Filter;
import ru.mpstat.FilterModel;
import ru.mpstat.api.PriceSegmentationResponse;
import ru.mpstat.api.SellersResponse;
import ru.mpstat.api.auth.AuthRequest;
import ru.mpstat.api.auth.AuthResponse;
import ru.mpstat.report.NicheReport;
import ru.mpstat.report.NicheReportByRevenue;
import ru.mpstat.report.NicheReportBySku;

@Log4j2
@RequiredArgsConstructor
@Command(name = "category-parser", description = "Update analytics data")
public class MpstatAnalyticService extends SeleniumBaseService implements Runnable {
  private int maxRequestCount = 200;
  private boolean byAPI = true;

  //  private static final String MPSTAT_HOST = "mpstats.shop";
  private static final String MPSTAT_HOST = "mpsklad.io";

  private static final String MPSTAT_LOGIN = "";
  private static final String MPSTAT_PASSWORD = "";

  private final String SPREADSHEET_ID = "";

  private String tempDir = "./Downloads";

  private static final String MPSTAT_BASE_URL = "https://" + MPSTAT_HOST;
  private String sessionKey;
  private final LocalDate now = LocalDate.now();


  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final int AWAIT_ELEMENT_TIME_SEC = 300;
  public static final int AWAIT_DOWNLOAD_SEC = 300;
  private final WebDriverWait wait =
      new WebDriverWait(driver, Duration.ofSeconds(AWAIT_ELEMENT_TIME_SEC));
  private final FilterModel filterModel = FilterModel.builder()
      .revenue(Filter.builder()
          .filterType("number")
          .type("greaterThanOrEqual")
          .filter("500000")
          .build())
      .price(Filter.builder()
          .filterType("number")
          .type("inRange")
          .filter("1800")
          .filterTo("500")
          .build())
      .build();


  @Option(type = OptionType.COMMAND,
      name = {"--categories-file", "-cf"},
      description = "Path to mpstats category list",
      title = "--categories-file <PATH_TO_JSON>")
  protected String categoryFilePath;

  @Option(type = OptionType.COMMAND,
      name = {"--categories-list", "-cl"},
      description = "Path to mpstats category list",
      title = "--categories-list <PATH_TO_List>")
  protected String categoryListPath;

  @Option(type = OptionType.COMMAND,
      name = {"--categories", "-c"},
      description = "List of categories",
      title = "--categories \"category name\"")
  protected List<String> categoriesFilter = new ArrayList<>();
  @Option(name = {"-d", "--download-categories"},
      description = "Download categories from mpstat")
  private boolean downloadCategories = false;

  @Option(name = {"-p", "--print-categories"},
      description = "Print categories from mpstat")
  private boolean printCategories = false;

  @Option(name = {"-r", "--revenue"},
      description = "Get categories revenue")
  private boolean revenue = false;

  @Override
  public void process() throws IOException {

    List<Category> categories = Optional.ofNullable(categoryFilePath)
        .map(this::readCategoriesFromResource)
        .orElseThrow(() -> new IllegalArgumentException("no categories was provided"));

    List<String> categoryList = getCategoryList(categories);

    if (printCategories) {
      FileUtils.writeLines(new File(tempDir + "/categoryList.txt"), categoryList);
      return;
    }

    if (downloadCategories) {
      getCategoriesFromMpStata(categories, categoryList);
      writeFileWithCategories(categories);
      return;
    }

    if (byAPI) {
      sessionKey = getSessionKey();
    }
    List<NicheReport> reports = categoryList.stream()
        .map(this::getCategoryUri).filter(Objects::nonNull)
        .filter(uriBuilder -> {
          if (maxRequestCount == 0) {
            log.warn("Max requests count has been reached");
            return false;
          } else {
            return true;
          }
        })
        .map(uriBuilder -> {
          if (byAPI) {
            return Collections.singletonList(uriBuilder);
          } else {
            return getUriBuildersFromWeb(uriBuilder, categories);
          }
        })
        .peek(uriBuilders -> log.info("will be proceed {} categories", uriBuilders.size()))
        .flatMap(Collection::stream)
        .map(this::getNicheReport)
        .collect(Collectors.toList());

    writeFileWithCategories(categories);
    updateNicheReport(reports, revenue);
  }

  private String getSessionKey() throws JsonProcessingException, UnsupportedEncodingException {
    final String json =
        objectMapper.writeValueAsString(new AuthRequest(MPSTAT_LOGIN, MPSTAT_PASSWORD));

    final HttpPost httpPost = new HttpPost(MPSTAT_BASE_URL + "/login");
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setEntity(new StringEntity(json));

    AuthResponse result =
        Optional.ofNullable(getApiResult(httpPost, new TypeReference<AuthResponse>() {
        })).orElseThrow(
            () -> new IllegalStateException("mpstatus authentication failure. response is empty"));

    if (!"success".equalsIgnoreCase(result.getStatus())) {
      throw new IllegalStateException(
          "mpstatus authentication failure. status: " + result.getStatus());
    }

    return result.getMessage();
  }

  @SneakyThrows
  private <T> T getApiResult(HttpRequestBase request, TypeReference<T> typeReference) {
    log.info("{} request: {}", request.getMethod(), request.getURI().toASCIIString());
    T result = null;
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      String response = client.execute(request, new BasicResponseHandler());
      result = objectMapper.readValue(response, typeReference);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return result;
  }

  private List<URIBuilder> getUriBuildersFromWeb(URIBuilder uriBuilder, List<Category> categories) {
    try {
      String url = uriBuilder.build().toASCIIString();
      log.info("getting url: {}", url);
      driver.get(url);
      maxRequestCount--;
      log.info("requestsLeft: {}", maxRequestCount);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    List<WebElement> subcategoriesDropDown =
        driver.findElements(By.cssSelector("i.fa-caret-down"));
    String categoryTree = getCategoryPathFromUrl(uriBuilder).orElseThrow();
    if (!subcategoriesDropDown.isEmpty() &&
        subcategoriesDropDown.get(0).isDisplayed()) {
      List<String> subCategoryList = driver.findElements(
              By.xpath("//a[@role='menuitem'][contains(@href,'/ozon/category')]")).stream()
          .map(webElement -> webElement.getAttribute("textContent"))
          .collect(Collectors.toList());
      updateCategories(categoryTree, categories, subCategoryList);
      return subCategoryList.stream().map(s -> categoryTree + "/" + s)
          .map(this::getCategoryUri)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } else {
      return Collections.singletonList(uriBuilder);
    }
  }

  private List<String> getCategoryList(List<Category> categories) throws IOException {
    List<String> categoryFilter;
    if (categoryListPath != null) {
      categoryFilter = FileUtils.readLines(new File(categoryListPath), StandardCharsets.UTF_8);
    } else {
      categoryFilter = new ArrayList<>();
    }
    return categories.stream().map(category ->
            category.getCategoriesTrees("").stream()
                .filter(categoriesTree -> categoriesFilter.isEmpty() ||
                    categoriesFilter.stream().anyMatch(categoriesTree::contains))
                .collect(Collectors.toList()))
        .flatMap(Collection::stream)
        .filter(s -> categoryFilter.isEmpty() || categoryFilter.stream().anyMatch(s::contains))
        .collect(Collectors.toList());
  }

  @SneakyThrows
  private void updateNicheReport(List<NicheReport> reports, boolean revenue) {
    GoogleSheetsService googleSheetsService =
        new GoogleSheetsService(SPREADSHEET_ID);
    String sheetName = revenue ? "ниши по выручке" : "ниши по товарам";

    googleSheetsService.updateNicheSheet(reports, sheetName);
  }

  private static Optional<String> getCategoryPathFromUrl(URIBuilder uriBuilder) {
    return uriBuilder.getQueryParams().stream()
        .filter(pair -> pair.getName().equals("url"))
        .map(NameValuePair::getValue)
        .findAny();
  }

  @SneakyThrows
  private NicheReport getNicheReport(URIBuilder uriBuilder) {
    String categoryPath = getCategoryPathFromUrl(uriBuilder).orElseThrow();
    log.info("{} left. {}...", maxRequestCount, categoryPath);

    NicheReport.NicheReportBuilder<?, ?> nicheReportBuilder;

    if (maxRequestCount == 0) {
      log.warn("Max requests count has been reached");
      nicheReportBuilder = NicheReport.builder();
    } else if (byAPI) {
      nicheReportBuilder = getNicheReportBuilderByAPI(categoryPath);
    } else {
      nicheReportBuilder = getNicheReportBuilderByWeb(uriBuilder, categoryPath);
    }

    nicheReportBuilder.categoryTree(categoryPath);
    nicheReportBuilder.url(uriBuilder.build().toASCIIString());
    return nicheReportBuilder.build();
  }

  private NicheReport.NicheReportBuilder<?, ?> getNicheReportBuilderByAPI(String categoryPath)
      throws URISyntaxException {
    NicheReportByRevenue.NicheReportByRevenueBuilder<?, ?> revenueBuilder =
        NicheReportByRevenue.builder();
    if (revenue) {
      try {
        getRevenueSegments(categoryPath, revenueBuilder);
        getTopSeller(categoryPath, revenueBuilder);
      } catch (IllegalArgumentException | IllegalStateException ex) {
        log.error(ex.getMessage());
      }
    }
    return revenueBuilder;
  }

  private void getRevenueSegments(String categoryPath,
                                  NicheReportByRevenue.NicheReportByRevenueBuilder<?, ?> revenueBuilder)
      throws URISyntaxException {
    maxRequestCount--;
    final HttpGet
        httpGetPriceSegmentations = new HttpGet(
        new URIBuilder()
            .setScheme("https")
            .setHost(MPSTAT_HOST)
            .setPath("/api/oz/get/category/price_segmentation")
            .setParameter("d1",
                now.minusMonths(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .setParameter("d2", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .setParameter("path", categoryPath)
            .build());
    httpGetPriceSegmentations.setHeader("Accept", "application/json");
    httpGetPriceSegmentations.setHeader("Cookie", "auth=" + sessionKey);

    List<PriceSegmentationResponse> priceSegmentationResponList =
        Optional.ofNullable(getApiResult(httpGetPriceSegmentations,
            new TypeReference<List<PriceSegmentationResponse>>() {
            })).orElseThrow(() -> new IllegalStateException(
            categoryPath + ": getting PriceSegmentation failure. response is empty"));

    PriceSegmentationResponse maxRevenueSegment = Collections.max(priceSegmentationResponList,
        Comparator.comparing(PriceSegmentationResponse::getRevenue));
    revenueBuilder.priceUntilByRevenue(
        Integer.parseInt(maxRevenueSegment.getRange().split("-")[1]));
    revenueBuilder.revenueByRevenue(maxRevenueSegment.getRevenue());
    revenueBuilder.lostRevenueByRevenue(maxRevenueSegment.getLostProfitPercent());

    PriceSegmentationResponse maxLostRevenueSegment = Collections.max(priceSegmentationResponList,
        Comparator.comparing(PriceSegmentationResponse::getLostProfitPercent));
    revenueBuilder.priceUntilByLostRevenue(
        Integer.parseInt(maxLostRevenueSegment.getRange().split("-")[1]));
    revenueBuilder.revenueByLostRevenue(maxLostRevenueSegment.getRevenue());
    revenueBuilder.lostRevenueByLostRevenue(maxLostRevenueSegment.getLostProfitPercent());
  }

  private void getTopSeller(String categoryPath,
                            NicheReportByRevenue.NicheReportByRevenueBuilder<?, ?> revenueBuilder)
      throws URISyntaxException {
    maxRequestCount--;
    final HttpGet
        httpGetSeller = new HttpGet(
        new URIBuilder()
            .setScheme("https")
            .setHost(MPSTAT_HOST)
            .setPath("/api/oz/get/category/sellers")
            .setParameter("d1",
                now.minusMonths(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .setParameter("d2", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .setParameter("path", categoryPath)
            .build());
    httpGetSeller.setHeader("Accept", "application/json");
    httpGetSeller.setHeader("Cookie", "auth=" + sessionKey);

    List<SellersResponse> sellersResponseList =
        Optional.ofNullable(getApiResult(httpGetSeller,
            new TypeReference<List<SellersResponse>>() {
            })).orElseThrow(() -> new IllegalStateException(
            categoryPath + ": getting SellersResponse failure. response is empty"));

    sellersResponseList.stream().filter(sellersResponse -> sellersResponse.getPosition() == 1)
        .findAny()
        .ifPresent(topSeller -> {
          int totalRevenue =
              sellersResponseList.stream().mapToInt(SellersResponse::getRevenue).sum();
          int totalSales = sellersResponseList.stream().mapToInt(SellersResponse::getSales).sum();
          revenueBuilder.topSellerName(topSeller.getName());
          revenueBuilder.topSellerRevenuePercent(
              ((double) topSeller.getRevenue() / totalRevenue) * 100);
          revenueBuilder.topSellerSalesPercent(
              ((double) topSeller.getSales() / totalSales) * 100);
        });
  }

  private NicheReport.NicheReportBuilder<?, ?> getNicheReportBuilderByWeb(URIBuilder uriBuilder,
                                                                          String categoryPath)
      throws URISyntaxException, InterruptedException, IOException, ParseException {
    File reportFile = getReportFile(uriBuilder, categoryPath);

    NicheReport.NicheReportBuilder nicheReportBuilder = NicheReport.builder();
    if (reportFile != null) {

      String fileExtension = Files.getFileExtension(reportFile.getName());

      try (ZipFile zipFile = fileExtension.endsWith("zip") ? new ZipFile(reportFile) : null;
           InputStream is = fileExtension.endsWith("zip") ?
               zipFile.getInputStream(zipFile.entries().nextElement()) :
               new FileInputStream(reportFile);
           Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        if (revenue) {
          nicheReportBuilder = getNicheReportByRevenue(reader);
        } else {
          nicheReportBuilder = getNicheReportBySku(reader);
        }
      }
    }
    return nicheReportBuilder;
  }

  private File getReportFile(URIBuilder uriBuilder, String categoryPath)
      throws URISyntaxException, InterruptedException, IOException {
    maxRequestCount--;
    driver.get(uriBuilder.build().toASCIIString());

    File result;
    if (revenue) {
      result = downloadReportByRevenue(categoryPath);
    } else {
      result = downloadReportBySkus(categoryPath);
    }
    if (result == null) {
      log.warn("data is empty. {}", categoryPath);
    }
    return result;
  }

  private boolean isTableIsNotEmpty() {
    wait.until(
        ExpectedConditions.or(
            ExpectedConditions.numberOfElementsToBe(
                By.xpath("//div[@ref='eOverlayWrapper']/div[@class='mp-loader-static']"), 1),
            ExpectedConditions.numberOfElementsToBe(
                By.xpath("//div[@ref='eOverlayWrapper']/div[contains (text(), 'Нет данных')]"),
                1),
            ExpectedConditions.numberOfElementsToBe(By.xpath(
                    "//div[@ref='leftContainer'][@class='ag-pinned-left-cols-container']/div[1]"),
                1)
        )
    );

    wait.until(ExpectedConditions.numberOfElementsToBe(
        By.xpath("//div[@ref='eOverlayWrapper']/div[@class='mp-loader-static']"), 0));

    return driver.findElements(
            By.xpath("//div[@ref='eOverlayWrapper']/div[contains (text(), 'Нет данных')]"))
        .isEmpty();
  }

  private File downloadReportByRevenue(String categoryPath)
      throws InterruptedException, IOException {

    if (maxRequestCount < 2) {
      log.warn("Max requests count has been reached");
      return null;
    }
    File segmentationFile = null;
    String fileName = renameFileReport(categoryPath + "/segmentation");
    List<File> alreadyDownloadedFiles = getFilesByName(fileName);
    if (!alreadyDownloadedFiles.isEmpty()) {
      log.info("File {} already downloaded", fileName);
      segmentationFile = alreadyDownloadedFiles.get(0);
    }
    if (segmentationFile == null) {
      WebElement segmentationTabButton = wait.until(ExpectedConditions.elementToBeClickable(
          By.xpath("//div[@role='tab']/span[ contains (text(), 'Ценовая сегментация')]")));
      while (segmentationTabButton.findElement(By.xpath("./..")).getAttribute("aria-selected")
          .equalsIgnoreCase("false")) {
        Actions actions = new Actions(driver);
        actions.moveToElement(segmentationTabButton);
        actions.perform();
        actions.click();
        segmentationTabButton.click();
      }
      wait.until(ExpectedConditions.numberOfElementsToBe(By.xpath("//canvas"), 1));

      boolean tableIsNotEmpty = isTableIsNotEmpty();

      if (tableIsNotEmpty) {
        maxRequestCount--;
        segmentationFile = getSegmentationFile(categoryPath);
      }
    }

    if (segmentationFile != null) {
      File sellersFile =
          getReportFileFromTab("//div[@role='tab']/span[ contains (text(), 'Продавцы')]",
              categoryPath + "/sellers");

      return combineFiles(segmentationFile, sellersFile);
    } else {
      return null;
    }
  }

  private File getReportFileFromTab(String xpathExpression, String fileName)
      throws InterruptedException {
    fileName = renameFileReport(fileName);
    List<File> alreadyDownloadedFiles = getFilesByName(fileName);
    if (!alreadyDownloadedFiles.isEmpty()) {
      log.info("File {} already downloaded", fileName);
      return alreadyDownloadedFiles.get(0);
    } else {
      maxRequestCount--;
      WebElement tabButton = wait.until(ExpectedConditions.elementToBeClickable(
          By.xpath(xpathExpression)));
      while (tabButton.findElement(By.xpath("./..")).getAttribute("aria-selected")
          .equalsIgnoreCase("false")) {
        Actions actions = new Actions(driver);
        actions.moveToElement(tabButton);
        actions.perform();
        tabButton.click();
      }

      boolean tableIsNotEmpty = isTableIsNotEmpty();
      if (tableIsNotEmpty) {
        clickDownloadButton();
        return awaitingToDownloadingFile(fileName, "(dd.MM.yyyy)");
      } else {
        log.warn("couldn't download file {}. table is empty", fileName);
        return null;
      }
    }
  }

  private File getSegmentationFile(String categoryPath) throws InterruptedException {
    String fileName = renameFileReport(categoryPath + "/segmentation");
    maxRequestCount--;
    clickDownloadButton();
    return awaitingToDownloadingFile(fileName, "(dd.MM.yyyy)");
  }

  public File combineFiles(File segmentationFile, File sellersFile)
      throws IOException {
    try (
        CSVParser segmentationFileParser = new CSVParser(new FileReader(segmentationFile),
            CSVFormat.Builder.create()
                .setDelimiter(";")
                .build());
        CSVParser sellersFileParser = sellersFile == null ? null :
            new CSVParser(new FileReader(sellersFile),
                CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .build())) {
      List<CSVRecord> segmentationFileRecords = segmentationFileParser.getRecords();
      segmentationFile.delete();

      Pair<String, String> topSeller = getTopSeller(sellersFileParser);

      List<List<String>> csvContent = new ArrayList<>();
      for (CSVRecord record : segmentationFileRecords) {
        long recordNumber = record.getRecordNumber();
        List<String> recordColumn = record.toList();
        if (recordNumber == 1L) {
          recordColumn.add(null);
          recordColumn.add(null);
        } else if (recordNumber == 2L) {
          recordColumn.add("Топ бренд, имя");
          recordColumn.add("Топ бренд, выручка");
          recordColumn.add("Топ продавец, имя");
          recordColumn.add("Топ продавец, выручка");
        } else {
          recordColumn.add(topSeller.getKey());
          recordColumn.add(topSeller.getValue());
        }
        csvContent.add(recordColumn);
      }

      writeCsv(segmentationFile.getAbsolutePath(), csvContent);
    }
    return segmentationFile;
  }

  private Pair<String, String> getTopSeller(CSVParser sellersFileParser) {
    if (sellersFileParser == null) {
      return new Pair<>("", "");
    }
    List<CSVRecord> records = sellersFileParser.getRecords();

    if (records.size() < 2) {
      return new Pair<>("", "");
    } else {
      CSVRecord firstRecord = records.get(1);
      return new Pair<>(firstRecord.get(1), firstRecord.get(7));
    }
  }


  public static void writeCsv(String filePath, List<List<String>> content) throws IOException {
    try (CSVPrinter filePrinter = new CSVPrinter(new FileWriter(filePath),
        CSVFormat.Builder.create()
            .setDelimiter(";")
            .build());) {
      for (List<String> record : content) {
        for (String s : record) {
          filePrinter.print(s != null ? s : "");
        }
        filePrinter.println();
      }
    }
  }

  private void clickDownloadButton() throws InterruptedException {
    WebElement exportButton = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//button[ contains (text(), 'Экспорт')]")));
    exportButton.click();

    WebElement downloadButton = wait.until(ExpectedConditions.numberOfElementsToBe(
        By.xpath("//button[contains(text(), 'Скачать' )]"), 2)).get(0);

    while (!downloadButton.isDisplayed()) {
      log.warn("DownloadButton is hidden!");
      exportButton.click();
      Thread.sleep(5000);
    }
    downloadButton = wait.until(ExpectedConditions.numberOfElementsToBe(
        By.xpath("//button[contains(text(), 'Скачать' )]"), 2)).get(0);
    downloadButton.click();
  }

  private File downloadReportBySkus(String categoryPath)
      throws InterruptedException {
    String categoryFileName = renameFileReport(categoryPath);

    List<File> alreadyDownloadedFiles = getFilesByName(categoryFileName);
    if (!alreadyDownloadedFiles.isEmpty()) {
      log.info("File {} already downloaded", categoryFileName);
      return alreadyDownloadedFiles.get(0);
    }

    boolean tableIsNotEmpty = isTableIsNotEmpty();
    File reportFile;
    if (tableIsNotEmpty) {
      WebElement exportButton = wait.until(ExpectedConditions.elementToBeClickable(
          By.xpath("//div[ contains (text(), 'Экспорт')]")));
      exportButton.click();
      if (maxRequestCount == 0) {
        log.warn("Max requests count has been reached");
        reportFile = null;
      } else {
        maxRequestCount--;
        WebElement downloadButton = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//button[contains(text(), 'Скачать все колонки' )]")));
        while (!downloadButton.isDisplayed()) {
          log.warn("DownloadButton is hidden!");
          exportButton.click();
          Thread.sleep(5000);
        }
        downloadButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[contains(text(), 'Скачать все колонки' )]")));
        downloadButton.click();
        reportFile = awaitingToDownloadingFile(categoryPath, "ddMMyyyy");
      }
    } else {
      reportFile = null;
    }
    return reportFile;
  }

  @SneakyThrows
  public File awaitingToDownloadingFile(String categoryPath, String dateFormatPattern) {
    String today = now.format(DateTimeFormatter.ofPattern(dateFormatPattern));
    List<File> reportFile = waitUntilFileWillBeDownloaded(AWAIT_DOWNLOAD_SEC, today + ".csv");

    if (reportFile.size() != 1) {
      log.error("reportFile for {} is {}", categoryPath, reportFile);
      return null;
    } else {
      File report = reportFile.get(0);
      String fileExtension = Files.getFileExtension(report.getName());
      File newFile =
          new File(report.getParent() + "/" + renameFileReport(categoryPath) + "." + fileExtension);
      boolean renameSuccess = report.renameTo(newFile);
      if (!renameSuccess) {
        log.error("failed with renaming report {} to {}", report.getName(), newFile.getName());
        return null;
      }
      return newFile;
    }
  }

  private static String renameFileReport(String categoryPath) {
    return categoryPath.replaceAll("[^a-zA-ZА-Яа-я\\d:]", "_");
  }

  public NicheReport.NicheReportBuilder getNicheReportByRevenue(Reader reader) throws IOException {
    CSVParser csvParser = new CSVParser(reader,
        CSVFormat.Builder.create()
            .setTrim(true)
            .setDelimiter(";")
            .build());

    int topRevenue = 0;
    double topRevenueLost = 0;
    int topRevenuePrice = 0;

    int topLostRevenue = 0;
    double topLostRevenueLost = 0;
    int topLostRevenuePrice = 0;

    String topSellerName = "";
    double topSellerRevenue = 0;

    List<CSVRecord> records = csvParser.getRecords();
    if (records.isEmpty()) {
      log.error("NicheReportByRevenue file is empty");
      return NicheReportByRevenue.builder();
    }
    for (CSVRecord record : records.subList(2, records.size())) {

      String revenueString = record.get(3).replaceAll("\\D", "");
      int revenueInt = Integer.parseInt(isEmpty(revenueString) ? "0" : revenueString);
      String revemueLostString = record.get(6).replaceAll("[^\\d\n,.]", "").replace(",", ".");
      double revenueLostDouble =
          Double.parseDouble(isEmpty(revemueLostString) ? "0" : revemueLostString);
      String revenuePrice = record.get(1).replaceAll("\\D", "");
      int revenuePriceInt = Integer.parseInt(isEmpty(revenuePrice) ? "0" : revenuePrice);
      if (revenueInt > topRevenue) {
        topRevenue = revenueInt;
        topRevenueLost =
            revenueLostDouble;
        topRevenuePrice = revenuePriceInt;
      }
      if (revenueLostDouble > topLostRevenueLost) {
        topLostRevenue = revenueInt;
        topLostRevenueLost = revenueLostDouble;
        topLostRevenuePrice = revenuePriceInt;
      }

      topSellerName = record.get(14);
      String topSellerRevenueStr = record.get(15).replaceAll("[^\\d\n,.]", "").replace(",", ".");
      topSellerRevenue =
          Double.parseDouble(isEmpty(topSellerRevenueStr) ? "0" : topSellerRevenueStr);
    }

    return NicheReportByRevenue.builder()
        .priceUntilByRevenue(topRevenuePrice)
        .revenueByRevenue(topRevenue)
        .lostRevenueByRevenue(topRevenueLost)
        .priceUntilByLostRevenue(topLostRevenuePrice)
        .revenueByLostRevenue(topLostRevenue)
        .lostRevenueByLostRevenue(topLostRevenueLost)
        .topSellerName(topSellerName)
        .topSellerRevenuePercent(topSellerRevenue);
  }

  private NicheReport.NicheReportBuilder getNicheReportBySku(Reader reader)
      throws IOException, ParseException {
    CSVParser csvParser = new CSVParser(reader,
        CSVFormat.Builder.create()
            .setHeader("SKU", "Name", "Category", "Схема", "Brand", "Seller", "Balance",
                "Balance FBS",
                "Comments", "Final price", "Max price", "Min price", "Average price",
                "Median price",
                "Цена с Ozon картой", "Sales", "Revenue", "Revenue potential", "Revenue average",
                "Lost profit", "Lost profit percent", "URL", "Thumb", "Days in stock",
                "Days with sales",
                "Average if in stock", "Rating", "FBS", "Base price", "Category Position",
                "Categories Last Count", "Sales Per Day Average", "Turnover", "Turnover days")
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setDelimiter(";")
            .build());

    List<CSVRecord> records = csvParser.getRecords();
    int countOfSku = records.size() - 1;
    int countWithMovement = 0;
    int countWithSales = 0;
    int salesTotal = 0;
    int revenueTotal = 0;
    int skuWithBalance = 0;

    List<Integer> medianPrices = new ArrayList<>();
    List<Double> ratings = new ArrayList<>();
    List<Double> turnovers = new ArrayList<>();
    for (CSVRecord record : records) {
      if (record.getRecordNumber() > 1) {
        int daysInStock = Integer.parseInt(record.get("Days in stock"));
        int daysWithSales = Integer.parseInt(record.get("Days with sales"));
        int sales = Integer.parseInt(record.get("Sales"));
        int revenue = Integer.parseInt(record.get("Revenue"));
        int balance = Integer.parseInt(record.get("Balance"));
        int medianPrice = Double.valueOf(record.get("Median price")).intValue();
        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        double rating = format.parse(record.get("Rating")).doubleValue();
        double turnover = format.parse(record.get("Turnover")).doubleValue();
        if (daysInStock > 0 && daysWithSales > 0) {
          countWithMovement++;
        }
        if (daysWithSales > 0) {
          countWithSales++;
          ratings.add(rating);
        }
        salesTotal += sales;
        revenueTotal += revenue;
        if (balance > 0) {
          skuWithBalance++;
        }
        medianPrices.add(medianPrice);
        turnovers.add(turnover);
      }
    }
    Double percentWithMovement = ((double) countWithMovement / max(countOfSku, 1)) * 100;
    Double percentWithSales = ((double) countWithSales / max(countOfSku, 1)) * 100;
    int medianPriceTotal = getMedian(medianPrices);
    double ratingWithSalesAverage = ratings.stream()
        .mapToDouble(Number::doubleValue)
        .average()
        .orElse(0);
    double turnoverAverage = turnovers.stream()
        .mapToDouble(Number::doubleValue)
        .average()
        .orElse(0);
    return NicheReportBySku.builder()
        .countOfSku(countOfSku)
        .countWithMovement(countWithMovement)
        .countWithSales(countWithSales)
        .salesTotal(salesTotal)
        .revenueTotal(revenueTotal)
        .skuWithBalance(skuWithBalance)
        .percentWithMovement(percentWithMovement)
        .percentWithSales(percentWithSales)
        .medianPriceTotal(medianPriceTotal)
        .ratingWithSalesAverage(ratingWithSalesAverage)
        .turnoverAverage(turnoverAverage);
  }

  public Integer getMedian(List<Integer> values) {
    Collections.sort(values);

    if (values.size() % 2 == 1) {
      return values.get((values.size() + 1) / 2 - 1);
    } else {
      int lower = values.get(values.size() / 2 - 1);
      int upper = values.get(values.size() / 2);

      return (lower + upper) / 2;
    }
  }

  private static void updateCategories(String categoryTree, List<Category> categories,
                                       List<String> subCategoryList) {
    List<Category> categoryList = categories;
    Category parentCategory = null;
    for (String categoryName : categoryTree.split("/")) {
      Optional<Category> filteredCategory = categoryList.stream()
          .filter(c -> c.getName().equals(categoryName))
          .findAny();
      if (filteredCategory.isPresent()) {
        parentCategory = filteredCategory.get();
        categoryList = parentCategory.getSubCategories();
      }
    }
    if (parentCategory != null) {
      for (String subCategoryName : subCategoryList) {
        parentCategory.addCategory(new Category(subCategoryName));
      }
    }
  }

  private URIBuilder getCategoryUri(String categoriesTree) {
    try {
      URIBuilder uriBuilder = new URIBuilder(MPSTAT_BASE_URL)
          .setPath("ozon/category")
          .addParameter("url", categoriesTree);
      if (!revenue) {
        uriBuilder.setFragment("filterModel=" + objectMapper.writeValueAsString(filterModel));
      }
      return uriBuilder;
    } catch (URISyntaxException | JsonProcessingException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  private List<Category> readCategoriesFromResource(String categoryFilePath) {
    Path path = Paths.get(categoryFilePath);
    try {
      File file = path.toFile();
      return objectMapper.readValue(file, new TypeReference<>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void writeFileWithCategories(List<Category> categories) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("categories.json"))) {
      writer.write(objectMapper.writeValueAsString(categories));
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void getCategoriesFromMpStata(List<Category> categories,
                                       List<String> categoryList) {
    if (maxRequestCount == 0) {
      log.warn("max request count has been reached");
      return;
    }

    categoryList.stream()
        .map(this::getCategoryUri).filter(Objects::nonNull)
        .filter(uriBuilder -> {
          if (maxRequestCount == 0) {
            log.warn("Max requests count has been reached");
            return false;
          } else {
            return true;
          }
        })
        .forEach(uriBuilder -> getUriBuildersFromWeb(uriBuilder, categories));
  }

  private String getSubCategory(int i, int j) {
    try {
      return driver.findElement(By.xpath(
          "//*[@id='rubrics-container']/div[" + i + "]/div[1]/div[2]/div[1]/ul[1]/li[" + j +
              "]/div[1]/label/span")).getAttribute("textContent");
    } catch (NoSuchElementException ex) {
      return driver.findElement(By.xpath(
              "//*[@id='rubrics-container']/div[" + i + "]/div[1]/div[2]/div[1]/ul[1]/li[" + j + "]"))
          .getAttribute("textContent");
    }
  }
}
