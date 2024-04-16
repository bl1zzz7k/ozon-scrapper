package ru.ozon.api;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BaseFilter {
    private DateFilter date;
}
