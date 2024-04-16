package ru.ozon.api.reports;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class Analytics {
    private List<AnalyticItem> analyticItems;
    public BigDecimal getSumSalesTotalAmount() {
        return analyticItems.stream().map(AnalyticItem::getRevenue)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }
    public BigDecimal getSumExpensesDrrAmount() {
        return analyticItems.stream().map(AnalyticItem::getExpensesDrrAmount)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }
}
