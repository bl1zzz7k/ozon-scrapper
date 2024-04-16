package ru.ozon.api.service;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;
import graphql.com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import ru.mpstat.report.NicheReport;
import ru.mpstat.report.NicheReportByRevenue;
import ru.mpstat.report.NicheReportBySku;
import ru.ozon.api.dto.Cluster;
import ru.ozon.api.dto.Metrics;
import ru.ozon.api.reports.StockReport;
import ru.ozon.api.reports.StockReportItem;

@Log4j2
@RequiredArgsConstructor
public class GoogleSheetsService {
  protected static final String APPLICATION_NAME = "Google Sheets API Java HOPSY";
  protected static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  protected static final String TOKENS_DIRECTORY_PATH = "tokens";
  protected static final List<String> SCOPES = singletonList(SheetsScopes.SPREADSHEETS);
  protected static final String CREDENTIALS_FILE_PATH = "/credentials.json";
  public final String spreadsheetId;
  public final Sheets sheetsService;

  public GoogleSheetsService(String spreadsheetId) throws GeneralSecurityException, IOException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    final Credential credentials = getCredentials(HTTP_TRANSPORT);
    sheetsService =
        new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials).setApplicationName(
            APPLICATION_NAME).build();
    this.spreadsheetId = spreadsheetId;
  }

  public void updatePricesSheet(Metrics metrics, Integer sheetId) throws IOException {
    final String sheetName = "цены на товары";

    Sheets.Spreadsheets.Values spreadSheetsValues = getSpreadSheetsValues();
    List<List<Object>> rowValues = getRowValues(spreadSheetsValues, sheetName);

    List<ValueRange> values = new ArrayList<>();
    List<Request> requests = new ArrayList<>();

    values.add(new ValueRange().setRange(sheetName + "!J2")
        .setValues(singletonList(singletonList(metrics.getLogisticCoefficient()))));

    if (rowValues == null || rowValues.isEmpty()) {
      log.warn("No data found.");
    } else {
      Double oldLogisticCoef = Optional.ofNullable(rowValues.get(1).get('j' - 'a'))
          .map(Object::toString)
          .map(Double::parseDouble)
          .orElse(0d);
      requests.add(
          createNoteRequest(sheetId, 2, 'j' - 'a', oldLogisticCoef,
              Double.compare(metrics.getLogisticCoefficient(), oldLogisticCoef)));

      values.add(new ValueRange().setRange(sheetName + "!A" + (rowValues.size()))
          .setValues(singletonList(singletonList(LocalDate.now().format(DateTimeFormatter.ISO_DATE)))));

      for (int i = 1; i < rowValues.size(); i++) {
        List<Object> row = rowValues.get(i);
        String skuName = row.get(0).toString();
        final int rowIndex = i + 1;
        metrics.getSkuItems().stream()
            .filter(skuItem -> skuItem.getSkuName().equalsIgnoreCase(skuName))
            .findAny()
            .ifPresent(skuItem -> {

              values.add(new ValueRange().setRange(sheetName + "!E" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getSkuPrice()))));
              Double oldSkuPrice = Optional.ofNullable(row.get('e' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.split(" ")[0])
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'e' - 'a', oldSkuPrice,
                      Double.compare(skuItem.getSkuPrice(), oldSkuPrice)));

              double newDrr = skuItem.getDrr() / 100;
              values.add(new ValueRange().setRange(sheetName + "!G" + (rowIndex))
                  .setValues(singletonList(singletonList(newDrr))));
              Double oldDrr = Optional.ofNullable(row.get('g' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace("%", ""))
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'g' - 'a', oldDrr,
                      Double.compare(newDrr, oldDrr)));

              values.add(new ValueRange().setRange(sheetName + "!L" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getLogisticPrice()))));
              Double oldLogisticPrice = Optional.ofNullable(row.get('l' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.split(" ")[0])
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'l' - 'a', oldLogisticPrice,
                      Double.compare(skuItem.getLogisticPrice(), oldLogisticPrice)));

              values.add(new ValueRange().setRange(sheetName + "!M" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getLastMilePrice()))));
              Double oldLastMilePrice = Optional.ofNullable(row.get('m' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.split(" ")[0])
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'm' - 'a', oldLastMilePrice,
                      Double.compare(skuItem.getLastMilePrice(), oldLastMilePrice)));

              values.add(new ValueRange().setRange(sheetName + "!N" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getAcquiring()))));
              Double oldAcquiring = Optional.ofNullable(row.get('n' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.split(" ")[0])
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'n' - 'a', oldAcquiring,
                      Double.compare(skuItem.getAcquiring(), oldAcquiring)));

              values.add(new ValueRange().setRange(sheetName + "!O" + (rowIndex)).setValues(
                  singletonList(
                      singletonList(format("=E%d*%f", rowIndex, skuItem.getSalesCommission())))));
              double newCommission = skuItem.getSkuPrice() * skuItem.getSalesCommission();
              Double oldCommission = Optional.ofNullable(row.get('o' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.split(" ")[0])
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'o' - 'a', oldCommission,
                      Double.compare(oldCommission, newCommission)));

              values.add(new ValueRange().setRange(sheetName + "!T" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getViews()))));
              Integer oldViews = Optional.ofNullable(row.get('t' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace(",", ""))
                  .map(Integer::parseInt)
                  .orElse(0);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 't' - 'a', oldViews,
                      Integer.compare(skuItem.getViews(), oldViews)));

              values.add(new ValueRange().setRange(sheetName + "!U" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getPosition()))));
              Integer oldPosition = Optional.ofNullable(row.get('u' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace(",", ""))
                  .map(Integer::parseInt)
                  .orElse(0);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'u' - 'a', oldPosition,
                      Integer.compare(oldPosition, skuItem.getPosition())));

              values.add(new ValueRange().setRange(sheetName + "!V" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getUctr()))));
              Double oldUctr = Optional.ofNullable(row.get('v' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace("%", ""))
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'v' - 'a', oldUctr,
                      Double.compare(skuItem.getUctr(), oldUctr)));

              values.add(new ValueRange().setRange(sheetName + "!w" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getConversionInCart() / 100))));
              Double oldConversionInCard = Optional.ofNullable(row.get('w' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace("%", ""))
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'w' - 'a', oldConversionInCard,
                      Double.compare(skuItem.getConversionInCart(), oldConversionInCard)));

              values.add(new ValueRange().setRange(sheetName + "!x" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getConversionInSales()))));
              Double oldConversionInSales = Optional.ofNullable(row.get('x' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace("%", ""))
                  .map(Double::parseDouble)
                  .orElse(0d);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'x' - 'a', oldConversionInSales,
                      Double.compare(skuItem.getConversionInSales(), oldConversionInSales)));

              values.add(new ValueRange().setRange(sheetName + "!Y" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getSales()))));
              Integer oldSales = Optional.ofNullable(row.get('y' - 'a'))
                  .map(Object::toString)
                  .map(s -> s.replace(",", ""))
                  .map(Integer::parseInt)
                  .orElse(0);
              requests.add(
                  createNoteRequest(sheetId, rowIndex, 'y' - 'a', oldSales,
                      Integer.compare(skuItem.getSales(), oldSales)));

              values.add(new ValueRange().setRange(sheetName + "!Z" + (rowIndex))
                  .setValues(singletonList(singletonList(skuItem.getStock()))));

              requests.add(createNoteRequest(sheetId, rowIndex, 0, skuItem.getPriceIndex(),
                  Objects.equals(skuItem.getPriceIndex(), "PROFIT") ? 1 : -1  ));
            });
      }
    }
    batchUpdateSpreadsheet(requests);
    batchUpdateValues(values, spreadSheetsValues);
  }

  public Sheets.Spreadsheets.Values getSpreadSheetsValues() {
    return sheetsService.spreadsheets().values();
  }

  private void batchUpdateSpreadsheet(List<Request> requests) throws IOException {
    BatchUpdateSpreadsheetRequest batchRequest =
        new BatchUpdateSpreadsheetRequest().setRequests(requests);
    sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
  }

  private Request createNoteRequest(Integer sheetId, int rowIndex, int columnIndex,
                                    @Nonnull Object note,
                                    int comparing) {
    Color color = new Color()
        .setRed(1f)
        .setGreen(1f)
        .setBlue(1f);
    if (comparing == 1) {
      color.setRed(0.5f)
          .setGreen(1f)
          .setBlue(0.5f);
    } else if (comparing == -1) {
      color.setRed(1f)
          .setGreen(0.5f)
          .setBlue(0.5f);
    }
    return new Request()
        .setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(rowIndex - 1)
                .setEndRowIndex(rowIndex)
                .setStartColumnIndex(columnIndex)
                .setEndColumnIndex(columnIndex + 1))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(color))
                .setNote(note.toString()))
            .setFields("note,userEnteredFormat.backgroundColor"));
  }

  private void batchUpdateValues(List<ValueRange> data,
                                 Sheets.Spreadsheets.Values spreadSheetsValues) throws IOException {
    BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
        .setValueInputOption("USER_ENTERED")
        .setData(data);
    BatchUpdateValuesResponse updateResult = spreadSheetsValues.batchUpdate(spreadsheetId, body)
        .execute();
    log.debug("BatchUpdateValuesResponse: {}", updateResult);
  }

  public void updateNicheSheet(List<NicheReport> reports, String sheetName) throws IOException {
    final List<List<Object>> values = reports.stream()
        .filter(nicheReport -> nicheReport.getCategoryTree() != null)
        .map(nicheReport -> {
              String[] categoryTreeParts = nicheReport.getCategoryTree().split("/");
              ArrayList<Object> rowValues = Lists.<Object>newArrayList(
                  format("=HYPERLINK(\"%s\", \"%s\")", nicheReport.getUrl(),
                      categoryTreeParts[categoryTreeParts.length - 1])
              );
              if (nicheReport instanceof NicheReportBySku) {
                NicheReportBySku nr = (NicheReportBySku) nicheReport;
                rowValues.add(nr.getCountOfSku());
                rowValues.add(nr.getCountWithMovement());
                rowValues.add(nr.getCountWithSales());
                rowValues.add(String.format("%,.2f", nr.getPercentWithMovement()));
                rowValues.add(String.format("%,.2f", nr.getPercentWithSales()));
                rowValues.add(nr.getSalesTotal());
                rowValues.add(nr.getRevenueTotal());
                rowValues.add(nr.getSkuWithBalance());
                rowValues.add(String.format("%,.2f", nr.getTurnoverAverage()));
                rowValues.add(nr.getMedianPriceTotal());
                rowValues.add(String.format("%,.2f", nr.getRatingWithSalesAverage()));
                rowValues.add(format("=HYPERLINK(\"%s\", \"%s\")", nicheReport.getUrl(),
                    nicheReport.getCategoryTree()));
              } else if (nicheReport instanceof NicheReportByRevenue) {
                NicheReportByRevenue nr = (NicheReportByRevenue) nicheReport;
                rowValues.add(nr.getPriceUntilByRevenue());
                rowValues.add(nr.getRevenueByRevenue());
                rowValues.add(String.format("%,.2f", nr.getLostRevenueByRevenue()));
                rowValues.add(nr.getPriceUntilByLostRevenue());
                rowValues.add(nr.getRevenueByLostRevenue());
                rowValues.add(String.format("%,.2f", nr.getLostRevenueByLostRevenue()));
//                rowValues.add(nr.getTopBrandName());
//                rowValues.add(String.format("%,.2f", nr.getTopBrandPercent()));
                rowValues.add(nr.getTopSellerName());
                rowValues.add(String.format("%,.2f", nr.getTopSellerRevenuePercent()));
                rowValues.add(String.format("%,.2f", nr.getTopSellerSalesPercent()));
              }
              return rowValues;
            }
        ).collect(Collectors.toList());

    ValueRange content = new ValueRange().setValues(values);

    appendValues(sheetName, content);
  }

  private void appendValues(String sheetName, ValueRange content) throws IOException {
    AppendValuesResponse updateResult = sheetsService.spreadsheets().values()
        .append(spreadsheetId, sheetName + "!A:B", content)
        .setValueInputOption("USER_ENTERED")
        .setInsertDataOption("INSERT_ROWS")
        .setIncludeValuesInResponse(true)
        .execute();
    log.debug(updateResult);
  }

  /**
   * Creates an authorized Credential object.
   *
   * @param httpTransport The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport httpTransport)
      throws IOException {
    // Load client secrets.
    try (InputStream in = Optional.ofNullable(
            GoogleSheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH))
        .orElseThrow();
         InputStreamReader reader = new InputStreamReader(in)) {
      GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);

      // Build flow and trigger user authorization request.
      GoogleAuthorizationCodeFlow flow =
          new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
              SCOPES)
              .setDataStoreFactory(
                  new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
              .setAccessType("offline")
              .build();
      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
      return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user123");
    }
  }

  public void updateStocks(StockReport stockReport) throws IOException {
    final String sheetName = "остатки и склады";

    Sheets.Spreadsheets.Values spreadSheetsValues = getSpreadSheetsValues();
    List<List<Object>> rowValues = getRowValues(spreadSheetsValues, sheetName);

    if (rowValues.isEmpty()) {
      log.warn("rowValues is empty");
    } else {
      List<Cluster> clusters = new ArrayList<>();

      List<Object> clusterRow = rowValues.get(0);
      for (int i = 1; i < clusterRow.size(); i++) {
        String clusterName = clusterRow.get(i).toString();
        if (!isEmpty(clusterName)) {
          Cluster cluster = new Cluster(clusterName, i);
          for (int j = 1; j < rowValues.size(); j++) {
            List<Object> row = rowValues.get(j);
            if (row.size() - 1 >= i) {
              String warehouseName = row.get(i).toString();
              if (!isEmpty(warehouseName)) {
                cluster.addWarehouse(warehouseName);
              }
            }
          }
          clusters.add(cluster);
        }
      }

      stockReport.getItems().forEach(item -> {
        Cluster cluster = clusters.stream()
            .filter(c -> c.getWarehouses().contains(item.getWarehouseName()))
            .findFirst().orElseThrow();
        item.setCluster(cluster);
      });

      Map<Cluster, List<StockReportItem>> clusterListMap = stockReport.getItems().stream()
          .collect(Collectors.groupingBy(StockReportItem::getCluster));

      List<ValueRange> values = new ArrayList<>();

      clusterListMap.forEach((cluster, value) -> {
        Map<String, List<StockReportItem>> skuMap =
            value.stream().collect(Collectors.groupingBy(StockReportItem::getSkuName));
        skuMap.forEach((skuName, value1) -> {
          int skuAmount = value1.stream()
              .map(StockReportItem::getSkuAmount)
              .mapToInt(Integer::intValue)
              .sum();
          double averageIdc = value1.stream()
              .mapToDouble(StockReportItem::getIdc)
              .average()
              .orElse(0d);
          for (int i = 0; i < rowValues.size(); i++) {
            if (!rowValues.get(i).isEmpty()) {
              String firstColumn = rowValues.get(i).get(0).toString();
              if (firstColumn.equals(skuName)) {
                values.add(new ValueRange().setRange(
                        sheetName + "!" + (char) ('@' + (cluster.getColumnIndex() + 1)) + (i + 1))
                    .setValues(singletonList(Lists.newArrayList(skuAmount, averageIdc))));
              }
            }
          }
        });

      });
      batchUpdateValues(values, spreadSheetsValues);
    }
  }

  public List<List<Object>> getRowValues(Sheets.Spreadsheets.Values spreadSheetsValues,
                                         String sheetName) throws IOException {
    ValueRange response = spreadSheetsValues.get(spreadsheetId, sheetName).execute();
    return response.getValues();
  }
}
