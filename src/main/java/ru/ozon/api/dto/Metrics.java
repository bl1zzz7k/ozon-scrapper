package ru.ozon.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Metrics {
    private double logisticCoefficient;
    private List<SkuItem> skuItems = new ArrayList<>();

    public void addSkuItem(SkuItem skuItem) {
        skuItems.add(skuItem);
    }
}
