package ru.ozon.api.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticItem {
    private String skuName;
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal expensesDrrAmount = BigDecimal.ZERO;
    @Builder.Default
    private int orderedUnits = 0;
    private int uniqueUser;
    private int clicks;
    private int views;
    private int position;
    private double conversionInCart;
}
