package ru.mpstat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Filter {
    private String filterType;
    private String type;
    private String filter;
    private String filterTo;
}
