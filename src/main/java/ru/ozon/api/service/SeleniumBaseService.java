package ru.ozon.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public abstract class SeleniumBaseService implements Runnable {
  public final WebDriver driver;

  public SeleniumBaseService() {
    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
    driver = new ChromeDriver(options);
  }

  @Override
  public void run() {
    try {
      process();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      ex.printStackTrace();
    } finally {
      driver.quit();
    }
  }

  public abstract void process() throws Exception;
}
