package ru.ozon.api;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import ru.mpstat.report.NicheReport;
import ru.mpstat.report.NicheReportByRevenue;
import ru.ozon.api.service.MpstatAnalyticService;

class MpstatAnalyticServiceTest {
  MpstatAnalyticService service;

  public MpstatAnalyticServiceTest() {
    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
    service = new MpstatAnalyticService();
  }

  @SneakyThrows
  @Test
  void testAwaitingToDownloadingFile() {
    URIBuilder uriBuilder = new URIBuilder()
        .setHost("https://analitika-wb-ozon.pro")
        .setPath("ozon/category")
        .setParameter("url",
            "Дом и сад/Посуда и кухонные принадлежности/Кухонные принадлежности/Бумага, фольга и пакеты");
    service.awaitingToDownloadingFile(null, "(dd.MM.yyyy)");
  }

  @Test
  void test_parseReportFile() {
    File file = new File(
        "/home/dzorin/Downloads/OZON - Дом и сад_Посуда и кухонные принадлежности_К 07012024-05022024 06022024.csv");
    NicheReport result = service.parseReportFile(file, Files.getFileExtension(file.getName()));
    System.out.println(result);
  }

  @Test
  void combineFiles_test() throws IOException, InterruptedException {
    File f1 = new File(
        "/home/dzorin/Downloads/OZON - Дом и сад_Дача и сад_Биотуалеты и септики_Би_segments.csv");
    File f2 = new File(
        "/home/dzorin/Downloads/OZON - Дом и сад_Дача и сад_Биотуалеты и септики_Би_brands.csv");
    File f3 = new File(
        "/home/dzorin/Downloads/OZON - Дом и сад_Дача и сад_Биотуалеты и септики_Би_sellers.csv");
        File result = service.combineFiles(f1, f2);
  }

  @Test
  void getNicheReportByRevenue_test() throws IOException {
    try (
        InputStream is = new FileInputStream(
            "/home/dzorin/Downloads/OZON - Дом и сад_Дача и сад_Биотуалеты и септики_Би_segments.csv");
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      NicheReportByRevenue result = service.getNicheReportByRevenue(reader);
      System.out.println(result);
    }
  }
}