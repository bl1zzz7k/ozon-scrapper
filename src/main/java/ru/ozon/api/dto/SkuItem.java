package ru.ozon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkuItem {
    private String skuName;
    private double logisticPrice;
    private double lastMilePrice;
    private int acquiring;
    private double salesCommission;
    private double skuPrice;
    private double skuMyPrice;
    private double drr;
    private int sales;
    private int stock;
    private String priceIndex;
    private double conversionInCart;
    private double conversionInSales;
    private int views;
    private int position;
    private double uctr;
}
