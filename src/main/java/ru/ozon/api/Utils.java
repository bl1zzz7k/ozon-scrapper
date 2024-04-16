package ru.ozon.api;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
@Log4j2
public class Utils {
    private static final Path DOWNLOAD_DIR_PATH = Paths.get(System.getProperty("user.home") + "/Downloads");

    @SneakyThrows
    public static List<File> waitUntilFileWillBeDownloaded(int waitTimeSec, String fileName) {
        int i = waitTimeSec * 1000 / 300;
        while (i != 0) {
            List<File> fileList = getFilesByName(fileName);
            if (!fileList.isEmpty()) {
                return fileList;
            }
            Thread.sleep(300);
            i--;
        }
        log.error("{} wasn't downloaded", fileName);
        return Collections.EMPTY_LIST;
    }


    public static List<File> getFilesByName(String fileName) {
        Instant nowInstant = Instant.now();
        LocalDate now = LocalDate.ofInstant(nowInstant, ZoneId.systemDefault());
        return Arrays.stream(DOWNLOAD_DIR_PATH.toFile().listFiles((dir, name) -> name.contains(fileName) && !name.contains(".~lock")))
                .filter(file -> {
                    long lastModified = file.lastModified();
                    Instant instant = new Date(lastModified).toInstant();
                    LocalDate fileCreationLocalDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
                    return fileCreationLocalDate.isEqual(now);
                }).collect(Collectors.toList());
    }
}
