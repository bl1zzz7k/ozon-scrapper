package ru.mpstat.report;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class NicheReportBySku extends NicheReport {
    private Integer countOfSku;
    private Integer countWithMovement;
    private Integer countWithSales;
    private Double percentWithMovement;
    private Double percentWithSales;
    private Integer salesTotal;
    private Integer revenueTotal;
    private Integer skuWithBalance;
    private Integer medianPriceTotal;
    private Double ratingWithSalesAverage;
    private Double turnoverAverage;
}

