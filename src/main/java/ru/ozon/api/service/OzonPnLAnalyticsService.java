package ru.ozon.api.service;

import static ru.ozon.api.service.OzonBaseService.OZON_API_BASE_URL;
import static ru.ozon.api.service.OzonBaseService.OZON_API_CLIENT_ID;
import static ru.ozon.api.service.OzonBaseService.OZON_API_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import ru.ozon.api.BaseRequest;
import ru.ozon.api.CashFlowRequest;
import ru.ozon.api.DateFilter;
import ru.ozon.api.analytics.Transaction;
import ru.ozon.api.analytics.TransactionResult;
import ru.ozon.api.analytics.TransactionService;
import ru.ozon.api.finance.CashFlowDeliveryService;
import ru.ozon.api.finance.CashFlowDetail;
import ru.ozon.api.finance.CashFlowDetailServiceItem;
import ru.ozon.api.finance.CashFlowResult;
import ru.ozon.api.finance.TransactionFilter;
import ru.ozon.api.finance.realization.RealizationResult;
import ru.ozon.api.finance.realization.RealizationResultRow;
import ru.ozon.api.product.BaseResponse;

@Log4j2
@RequiredArgsConstructor
@Command(name = "pnl-report", description = "Get pnl report")
public class OzonPnLAnalyticsService implements Runnable {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Option(type = OptionType.COMMAND, name = {"--period", "-p"})
  protected String period;


  @Override
  public void run() {
    try {
      process();
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void process() throws JsonProcessingException {
    HttpPost httpPostForRealization = createHttpPostForRealization();

    RealizationResult result =
        Optional.ofNullable(
            getApiResult(httpPostForRealization, new TypeReference<BaseResponse<RealizationResult>>() {
            })).orElseThrow();
    int qty = result.getRows().stream().mapToInt(RealizationResultRow::getSaleQty).sum();
    double salesAmount = result.getRows().stream().mapToDouble(RealizationResultRow::getSaleAmount).sum();
    double saleCommission = result.getRows().stream().mapToDouble(RealizationResultRow::getSaleCommission).sum();
    double returnPrices = result.getRows().stream().mapToDouble(RealizationResultRow::getReturnPriceSeller).sum();

    log.info("RealizationResult");
    log.info("qty: {}, salesAmount: {}, saleCommission: {}, returnPrices: {}", qty,salesAmount,saleCommission,returnPrices );

    List<Transaction> transactions;

    HttpPost httpPostForTransactions = createHttpPostForTransactions(1);
    TransactionResult transactionResult =
        Optional.ofNullable(
            getApiResult(httpPostForTransactions, new TypeReference<BaseResponse<TransactionResult>>() {
            })).orElseThrow();
    transactions = transactionResult.getOperations();

    if (transactionResult.getPageCount() > 1) {
      for (int i = 2; i <= transactionResult.getPageCount(); i++) {
        HttpPost httpPostForTrans = createHttpPostForTransactions(i);
        log.info("transaction request {}", i);
        TransactionResult transResult =
            Optional.ofNullable(
                getApiResult(httpPostForTrans, new TypeReference<BaseResponse<TransactionResult>>() {
                })).orElseThrow();
        transactions.addAll(transResult.getOperations());
      }
    }

    Map<String, Double> map =
        transactions.stream().collect(Collectors.groupingBy(Transaction::getOperationType))
            .entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().mapToDouble(Transaction::getAmount).sum()
            ));

    Map<String, Double> tranSercMap =
        transactions.stream().map(Transaction::getServices).flatMap(Collection::stream)
            .collect(Collectors.groupingBy(TransactionService::getName))
            .entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().mapToDouble(TransactionService::getPrice).sum()
            ));


    log.info("transactions");
    map.forEach((s, aDouble) -> log.info("{}: {}", s, aDouble));
    tranSercMap.forEach((s, aDouble) -> log.info("{}: {}", s, aDouble));

    double salesCommission = transactions.stream().mapToDouble(Transaction::getSaleCommission).sum();
    double tranAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();
    double tranAccur = transactions.stream().mapToDouble(Transaction::getAccrualsForSale).sum();
    log.info("tranAmount: {}, tranAccur: {}, salesCommission: {}",tranAmount,tranAccur,  salesCommission);

    Double acquiringCost = map.get("MarketplaceRedistributionOfAcquiringOperation");
    Double stockDisposalCost = map.get("OperationMarketplaceServiceStockDisposal");
    Double inboundCargoSurplusCost = map.get("OperationMarketplaceServiceSupplyInboundCargoSurplus");
    Double clientReturnCost = map.get("ClientReturnAgentOperation");
    Double crossDockServiceCost = map.get("OperationMarketplaceCrossDockServiceWriteOff");
    Double marketingActionCost = map.get("MarketplaceMarketingActionCostOperation");
    Double premiumSubscriptionCost = map.get("OperationMarketplacePremiumSubscribtion");


    HttpPost httpPostForCashFlow = createHttpPostForCashFlow();
    CashFlowResult cashFlowResult =
        Optional.ofNullable(
            getApiResult(httpPostForCashFlow, new TypeReference<BaseResponse<CashFlowResult>>() {
            })).orElseThrow();
    double returnAmount = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getReturnService().getAmount()).sum();
    double returnTotal = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getReturnService().getTotal()).sum();

    double returnServiceTotal = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getReturnService().getServices().getTotal()).sum();

    double deliveryTotal = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getDelivery().getTotal()).sum();
    double deliveryAmount = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getDelivery().getAmount()).sum();
    double deliveryServiceTotal = cashFlowResult.getDetails().stream()
        .mapToDouble(value -> value.getDelivery().getServices().getTotal()).sum();

    Map<String, Double> deliveryMap = cashFlowResult.getDetails().stream().map(CashFlowDetail::getDelivery)
        .flatMap(i -> i.getServices().getItems().stream()).collect(Collectors.groupingBy(
            CashFlowDetailServiceItem::getName))
        .entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().mapToDouble(CashFlowDetailServiceItem::getPrice).sum()
        ));

    Map<String, Double> returnMap = cashFlowResult.getDetails().stream().map(CashFlowDetail::getReturnService)
        .flatMap(i -> i.getServices().getItems().stream()).collect(Collectors.groupingBy(
            CashFlowDetailServiceItem::getName))
        .entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().mapToDouble(CashFlowDetailServiceItem::getPrice).sum()
        ));

    Map<String, Double> serviceMap = cashFlowResult.getDetails().stream().map(CashFlowDetail::getServices)
        .map(CashFlowDeliveryService::getItems)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(
            CashFlowDetailServiceItem::getName))
        .entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().mapToDouble(CashFlowDetailServiceItem::getPrice).sum()
        ));

    Map<String, Double> otherMap = cashFlowResult.getDetails().stream().map(CashFlowDetail::getOthers)
        .map(CashFlowDeliveryService::getItems)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(
            CashFlowDetailServiceItem::getName))
        .entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().mapToDouble(CashFlowDetailServiceItem::getPrice).sum()
        ));


    log.info("CashFlow");

    log.info("returnAmount: {}, returnTotal:{}, returnServiceTotal: {}, deliveryTotal:{}, deliveryAmount: {}, deliveryServiceTotal: {}",
        returnAmount, returnTotal, returnServiceTotal, deliveryTotal, deliveryAmount, deliveryServiceTotal);

    log.info("deliveryMap");
    deliveryMap.forEach((s, aDouble) -> {
      log.info("{}: {}", s, aDouble);
    });

    log.info("returnMap");
    returnMap.forEach((s, aDouble) -> {
      log.info("{}: {}", s, aDouble);
    });

    log.info("serviceMap");
    serviceMap.forEach((s, aDouble) -> {
      log.info("{}: {}", s, aDouble);
    });

    log.info("otherMap");
    otherMap.forEach((s, aDouble) -> {
      log.info("{}: {}", s, aDouble);
    });
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

  private HttpPost createHttpPostForCashFlow() throws JsonProcessingException {
    String path = "/v1/finance/cash-flow-statement/list";
    final HttpPost httpPost = createHttpPost(path);

    DateTimeFormatter fmt = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM")
        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
        .parseDefaulting(ChronoField.SECOND_OF_DAY, 0)
        .toFormatter();

    OffsetDateTime from = LocalDateTime.parse(period, fmt).atOffset(ZoneOffset.UTC);

    CashFlowRequest request = CashFlowRequest.builder()
            .date(
                DateFilter.builder()
                    .from(from.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                    .to(from.plusMonths(1).minusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                    .build()
            )
        .build();
    final String json = objectMapper.writeValueAsString(request);

    final StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
    httpPost.setEntity(entity);
    return httpPost;
  }

  private HttpPost createHttpPostForTransactions(int pageNumber)
      throws JsonProcessingException {
    String path = "/v3/finance/transaction/list";
    final HttpPost httpPost = createHttpPost(path);

    DateTimeFormatter fmt = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM")
        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
        .parseDefaulting(ChronoField.SECOND_OF_DAY, 0)
        .toFormatter();

    OffsetDateTime from = LocalDateTime.parse(period, fmt).atOffset(ZoneOffset.UTC);

    BaseRequest request = BaseRequest.builder()
        .filter(TransactionFilter.builder()
            .date(
                DateFilter.builder()
                    .from(from.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                    .to(from.plusMonths(1).minusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                    .build()
            )
            .build())
        .page(pageNumber)
        .build();
    final String json = objectMapper.writeValueAsString(request);

    final StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
    httpPost.setEntity(entity);
    return httpPost;
  }

  @SneakyThrows
  private HttpPost createHttpPostForRealization() {
    String path = "/v1/finance/realization";
    final HttpPost httpPost = createHttpPost(path);

    final String json = "{\"date\": \"" + period +"\"}";

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
}
