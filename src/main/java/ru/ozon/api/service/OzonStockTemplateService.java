package ru.ozon.api.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import com.google.api.services.sheets.v4.Sheets;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import ru.ozon.api.dto.SupplyItem;

@Log4j2
@RequiredArgsConstructor
@Command(name = "export-templates", description = "Update stock data")
public class OzonStockTemplateService implements Runnable {
  private final String SPREADSHEET_ID = "1CniVpLR6YOx9PxiI58b3LOBR-xONiV2gQz9e8FufKP4";
  private static final String EXPORT_PATH = "/home/dzorin/Downloads/ozon";

  @Option(type = OptionType.COMMAND,
      name = {"--sku", "-s"},
      description = "List of sku's",
      title = "--s \"sku name\"")
  protected List<String> skuFilter = new ArrayList<>();


  @Override
  public void run() {
    try {
      FileUtils.cleanDirectory(new File(EXPORT_PATH));

      final String sheetName = "поставка";
      GoogleSheetsService service = new GoogleSheetsService(SPREADSHEET_ID);
      Sheets.Spreadsheets.Values spreadSheetsValues = service.getSpreadSheetsValues();
      List<List<Object>> rows = service.getRowValues(spreadSheetsValues, sheetName);

      Map<String, List<SupplyItem>> supplies = new HashMap<>();

      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        List<Object> row = rows.get(rowIndex);
        if (!row.isEmpty() && !isBlank(row.get(0).toString()) && !row.get(0).equals("артикул")) {
          String skuName = row.get(0).toString();
          if (!skuFilter.isEmpty() && !skuFilter.contains(skuName)) {
            continue;
          }
          String amount;
          for (int columnIndex = 5; columnIndex < row.size(); columnIndex++) {
            amount = row.get(columnIndex).toString();
            if (!isBlank(amount) && isCreatable(amount)) {
              for (int i = rowIndex; i > 2; i--) {
                List<Object> prevRows = rows.get(i);
                if (prevRows.get(columnIndex).toString().equals("склад поставки")) {
                  String warehouse = rows.get(i).get(columnIndex + 1).toString();

                  List<SupplyItem> supplyItems =
                      supplies.getOrDefault(warehouse, new ArrayList<>());

                  int intAmount = Integer.parseInt(amount);

                  supplyItems.stream().filter(supplyItem -> supplyItem.getSkuName().equals(skuName))
                      .findFirst()
                      .ifPresentOrElse(supplyItem -> supplyItem.addAmount(intAmount),
                          () -> supplyItems.add(new SupplyItem(skuName, intAmount)));

                  supplies.put(warehouse, supplyItems);
                  break;
                }
              }
            }
          }

        }
      }
      log.info(new ObjectMapper().writeValueAsString(supplies));

      for (Map.Entry<String, List<SupplyItem>> entry : supplies.entrySet()) {
        String warehouse = entry.getKey();
        List<SupplyItem> supplyItems = entry.getValue();
        createXlsFiles(supplyItems, warehouse);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private static void createXlsFiles(List<SupplyItem> supplyItems, String warehouse) {
    try (Workbook wb = new HSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Sheet1");

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Артикул");
      headerRow.createCell(1).setCellValue("Имя (необязательно)");
      headerRow.createCell(2).setCellValue("Количество");

      for (SupplyItem supplyItem : supplyItems) {
        Row row = sheet.createRow(supplyItems.indexOf(supplyItem) + 1);
        row.createCell(0).setCellValue(supplyItem.getSkuName());
        row.createCell(2).setCellValue(supplyItem.getAmount());
      }

      File file = new File(EXPORT_PATH + "/" + warehouse + ".xlsx");
      try (OutputStream fileOut = Files.newOutputStream(file.toPath())) {
        wb.write(fileOut);
      } catch (Exception ex) {
        log.error(ex.getMessage(), ex);
      }
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}
