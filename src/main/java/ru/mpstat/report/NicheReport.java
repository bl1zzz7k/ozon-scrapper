package ru.mpstat.report;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class NicheReport {
    private String categoryTree;
    private String url;
}
