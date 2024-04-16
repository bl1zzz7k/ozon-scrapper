import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.help.Help;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import ru.ozon.api.service.MpstatAnalyticService;
import ru.ozon.api.service.OzonPnLAnalyticsService;
import ru.ozon.api.service.OzonSkuAnalyticsService;
import ru.ozon.api.service.OzonStockService;
import ru.ozon.api.service.OzonStockTemplateService;

@Log4j2
@Cli(name = "ozon-scrapper",
    description = "Ozon Scrapper Helper App",
    defaultCommand = Help.class,
    commands = {
        OzonSkuAnalyticsService.class,
        MpstatAnalyticService.class,
        OzonStockService.class,
        OzonStockTemplateService.class,
        OzonPnLAnalyticsService.class,
        Help.class
    })
public class OzonScrapperApp {

  public static void main(String[] args) {
    log.info("Ozon scrapper has been started with args: {}", Arrays.toString(args));
    try {
      com.github.rvesse.airline.Cli<Runnable> cli =
          new com.github.rvesse.airline.Cli<>(OzonScrapperApp.class);
      Runnable cmd = cli.parse(args);
      cmd.run();
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      log.info("Ozon scrapper has been finished");
    }
  }
}
