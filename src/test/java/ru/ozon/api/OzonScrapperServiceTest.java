package ru.ozon.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ru.ozon.api.product.BaseResponse;
import ru.ozon.api.product.PricesResult;
import ru.ozon.api.product.StockResult;

class OzonScrapperServiceTest {

    @Test
    void test() {
        String stockResponse = "{\n" +
                "  \"result\": {\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"sku\": 1085714559,\n" +
                "        \"warehouse_name\": \"ХОРУГВИНО_РФЦ\",\n" +
                "        \"item_code\": \"kovrik\",\n" +
                "        \"item_name\": \"Тефлоновый коврик 3 ШТУКИ, для выпечки и запекания в духовке, 30х40 см, антипригарный многоразовый термостойкий для гриля\",\n" +
                "        \"promised_amount\": 0,\n" +
                "        \"free_to_sell_amount\": 0,\n" +
                "        \"reserved_amount\": 14\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        String priceResponse = "{\n" +
                "  \"result\": {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"product_id\": 576419806,\n" +
                "        \"offer_id\": \"kovrik\",\n" +
                "        \"price\": {\n" +
                "          \"price\": \"445.0000\",\n" +
                "          \"old_price\": \"2450.0000\",\n" +
                "          \"premium_price\": \"\",\n" +
                "          \"recommended_price\": \"\",\n" +
                "          \"retail_price\": \"0.0000\",\n" +
                "          \"vat\": \"0.0\",\n" +
                "          \"min_ozon_price\": \"\",\n" +
                "          \"marketing_price\": \"345.0000\",\n" +
                "          \"marketing_seller_price\": \"345.0000\",\n" +
                "          \"min_price\": \"0.0000\",\n" +
                "          \"currency_code\": \"RUB\",\n" +
                "          \"auto_action_enabled\": false\n" +
                "        },\n" +
                "        \"price_index\": \"0.00\",\n" +
                "        \"commissions\": {\n" +
                "          \"sales_percent\": 20,\n" +
                "          \"fbo_fulfillment_amount\": 0,\n" +
                "          \"fbo_direct_flow_trans_min_amount\": 0,\n" +
                "          \"fbo_direct_flow_trans_max_amount\": 0,\n" +
                "          \"fbo_deliv_to_customer_amount\": 18.98,\n" +
                "          \"fbo_return_flow_amount\": 0,\n" +
                "          \"fbo_return_flow_trans_min_amount\": 63,\n" +
                "          \"fbo_return_flow_trans_max_amount\": 63,\n" +
                "          \"fbs_first_mile_min_amount\": 0,\n" +
                "          \"fbs_first_mile_max_amount\": 25,\n" +
                "          \"fbs_direct_flow_trans_min_amount\": 76,\n" +
                "          \"fbs_direct_flow_trans_max_amount\": 76,\n" +
                "          \"fbs_deliv_to_customer_amount\": 18.98,\n" +
                "          \"fbs_return_flow_amount\": 0,\n" +
                "          \"fbs_return_flow_trans_min_amount\": 76,\n" +
                "          \"fbs_return_flow_trans_max_amount\": 76,\n" +
                "          \"sales_percent_fbo\": 19,\n" +
                "          \"sales_percent_fbs\": 20\n" +
                "        },\n" +
                "        \"marketing_actions\": {\n" +
                "          \"current_period_from\": \"2023-03-05T21:00:00Z\",\n" +
                "          \"current_period_to\": \"2024-03-12T20:59:59Z\",\n" +
                "          \"actions\": [\n" +
                "            {\n" +
                "              \"date_from\": \"2023-03-05T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"Еженедельная акция\",\n" +
                "              \"discount_value\": \"445.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2023-08-10T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-09T20:59:59Z\",\n" +
                "              \"title\": \"хоспи\",\n" +
                "              \"discount_value\": \"345.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-01-08T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"Хиты. Все категории. Зимняя распродажа одежды\",\n" +
                "              \"discount_value\": \"360.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-01-08T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"СуперХиты. Все категории. Зимняя распродажа одежды\",\n" +
                "              \"discount_value\": \"352.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-01-08T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"СуперХиты. Все категории. Зимняя распродажа одежды - 5%\",\n" +
                "              \"discount_value\": \"337.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-01-21T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"МегаХиты. Все категории. Зимняя распродажа одежды\",\n" +
                "              \"discount_value\": \"372.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-01-21T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-06T20:59:59Z\",\n" +
                "              \"title\": \"МегаХиты. Все категории. Зимняя распродажа одежды -5%\",\n" +
                "              \"discount_value\": \"351.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-02-06T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-03-12T20:59:59Z\",\n" +
                "              \"title\": \"Радуй и Удивляй распродажи. Хиты\",\n" +
                "              \"discount_value\": \"334.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-02-06T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-03-12T20:59:59Z\",\n" +
                "              \"title\": \"Радуй и Удивляй распродажи. СуперХиты\",\n" +
                "              \"discount_value\": \"334.0000\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"date_from\": \"2024-02-13T21:00:00Z\",\n" +
                "              \"date_to\": \"2024-02-20T20:59:59Z\",\n" +
                "              \"title\": \"Радуй распродажа. МегаХиты\",\n" +
                "              \"discount_value\": \"328.0000\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"ozon_actions_exist\": false\n" +
                "        },\n" +
                "        \"volume_weight\": 0.1,\n" +
                "        \"price_indexes\": {\n" +
                "          \"price_index\": \"PROFIT\",\n" +
                "          \"external_index_data\": {\n" +
                "            \"minimal_price\": \"1023.0000\",\n" +
                "            \"minimal_price_currency\": \"RUB\",\n" +
                "            \"price_index_value\": 0.34\n" +
                "          },\n" +
                "          \"ozon_index_data\": {\n" +
                "            \"minimal_price\": \"\",\n" +
                "            \"minimal_price_currency\": \"RUB\",\n" +
                "            \"price_index_value\": 0\n" +
                "          },\n" +
                "          \"self_marketplaces_index_data\": {\n" +
                "            \"minimal_price\": \"\",\n" +
                "            \"minimal_price_currency\": \"RUB\",\n" +
                "            \"price_index_value\": 0\n" +
                "          }\n" +
                "        },\n" +
                "        \"acquiring\": 5\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        PricesResult apiResult = getApiResult(priceResponse, new TypeReference<BaseResponse<PricesResult>>() {});
        System.out.println(apiResult);
    }

    @SneakyThrows
    private <T> T getApiResult(String response, TypeReference<BaseResponse<T>> typeReference) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

//        return clazz.getDeclaredConstructor().newInstance();
        return objectMapper.readValue(response, typeReference).getResult();
    }


}