package ru.mpstat.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class NicheReportByRevenue extends NicheReport {
    private int priceUntilByRevenue;
    private int revenueByRevenue;
    private double lostRevenueByRevenue;

    private int priceUntilByLostRevenue;
    private int revenueByLostRevenue;
    private double lostRevenueByLostRevenue;

    private String topSellerName;
    private double topSellerRevenuePercent;
    private double topSellerSalesPercent;
}

