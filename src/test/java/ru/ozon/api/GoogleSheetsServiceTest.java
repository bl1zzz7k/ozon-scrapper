package ru.ozon.api;

import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import graphql.com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import ru.ozon.api.service.GoogleSheetsService;

class GoogleSheetsServiceTest {
    private final GoogleSheetsService googleSheetsService = new GoogleSheetsService("1QssLsXfYxkbXWJHTRNVRQA3VgfC_wP8NZczh--M6dHA");
    GoogleSheetsServiceTest() throws GeneralSecurityException, IOException {

    }

    @SneakyThrows
    @Test
    void test(){
        ValueRange content = new ValueRange().setValues(Lists.newArrayList(
                Lists.newArrayList(1,2,3,4,5,6,7,8,9,10, 11, 12,13),
                Lists.newArrayList(9,8,7,6,5,4),
                Lists.newArrayList("=HYPERLINK(\"google.com\", \"ciao\")","b","c","d","e","f")
                ));

        AppendValuesResponse updateResult = googleSheetsService.sheetsService.spreadsheets().values()
                .append(googleSheetsService.spreadsheetId, "НИШИ!A:B", content)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .setIncludeValuesInResponse(true)
                .execute();
    }

}