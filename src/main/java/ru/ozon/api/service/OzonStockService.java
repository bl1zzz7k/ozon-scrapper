package ru.ozon.api.service;

import static ru.ozon.api.Utils.getFilesByName;
import static ru.ozon.api.Utils.waitUntilFileWillBeDownloaded;

import com.github.rvesse.airline.annotations.Command;
import com.google.common.base.Preconditions;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.ozon.api.reports.StockReport;
import ru.ozon.api.reports.StockReportItem;

@Log4j2
@RequiredArgsConstructor
@Command(name = "update-stock", description = "Update stock data")
public class OzonStockService extends OzonBaseService {
  private final String SPREADSHEET_ID = "1CniVpLR6YOx9PxiI58b3LOBR-xONiV2gQz9e8FufKP4";
  private final String STOCK_FILENAME = "stocks_and_movement_products-report";

  @Override
  public void process() throws Exception {
    getFilesByName(STOCK_FILENAME).forEach(File::delete);
    StockReport stockReport = getStockReport();
    GoogleSheetsService googleSheetsService = new GoogleSheetsService(SPREADSHEET_ID);
    googleSheetsService.updateStocks(stockReport);

  }

  private StockReport getStockReport() {
    driver.get(OZON_BASE_URL + "/fbo-operations/stocks-management/reports");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AWAIT_ELEMENT_TIME_SEC));

    wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//*[ contains (text(), 'Скачать отчёт' ) ]")));

    List<WebElement> downloadButtons =
        driver.findElements(By.xpath("//*[ contains (text(), 'Скачать отчёт' ) ]"));
    Preconditions.checkArgument(downloadButtons.size() == 3);

    downloadButtons.get(1).click();

    waitUntilFileWillBeDownloaded(AWAIT_DOWNLOAD_TIME_SEC, STOCK_FILENAME);

    List<File> stockReportFiles = getFilesByName(STOCK_FILENAME);
    if (stockReportFiles.size() > 1) {
      throw new IllegalArgumentException("stock report file size=" + stockReportFiles.size());
    } else if (stockReportFiles.isEmpty()) {
      log.warn("{} doesn't exists", STOCK_FILENAME);
    }

    return parseStockReportFile(stockReportFiles.get(0));
  }

  @SneakyThrows
  private StockReport parseStockReportFile(File stockReportFile) {
    try (Workbook workbook = new XSSFWorkbook(stockReportFile)) {
      Sheet sheet = workbook.getSheetAt(0);
      List<StockReportItem> items = new ArrayList<>();
      for (Row row : sheet) {
        if (row.getRowNum() > 3) {
          String skuName = row.getCell(2).toString();
          String warehouseName = row.getCell(1).toString();
          String skuAmount = row.getCell(5).toString();
          String idc = row.getCell(7).toString();
          items.add(StockReportItem.builder()
              .skuName(skuName)
              .warehouseName(warehouseName)
              .skuAmount((int) Double.parseDouble(skuAmount))
              .idc(Double.parseDouble(idc))
              .build());
        }
      }
      return new StockReport(items);
    }
  }
}
