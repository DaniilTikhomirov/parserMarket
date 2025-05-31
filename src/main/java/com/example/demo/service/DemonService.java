package com.example.demo.service;

import com.example.demo.entity.Smartphone;
import com.example.demo.exeptions.FailureUrl;
import com.example.demo.repository.SmartphoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class DemonService {

    private static final String CARD_CSS = "div._1ENFO";
    private static final String LINK_CSS = "a[data-auto='snippet-link']";
    private static final String TITLE_CSS = "span[data-auto='snippet-title']";
    private static final String SPEC_BLOCK_CSS = "div._2Ce4O";
    private static final String SPEC_LABEL_CSS = "span.ds-text_color_text-secondary";
    private static final String SPEC_VALUE_CSS = "span.ds-text_color_text-primary";
    private static final String RATING_BLOCK_CSS = "span[data-auto='reviews']";
    private static final String RATING_VALUE_CSS = "span.ds-rating__value";

    @Value("${parser.url.start}")
    private String url;

    @Value("${parser.scrolls}")
    private int scrolls;

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final SmartphoneRepository smartphoneRepository;

    public DemonService(WebDriver driver, WebDriverWait wait, SmartphoneRepository smartphoneRepository) {
        this.driver = driver;
        this.wait = wait;
        this.smartphoneRepository = smartphoneRepository;
    }

    @Scheduled(fixedRateString = "${daemon.fixedRate:300000}")
    public void start() {
        long startTime = System.currentTimeMillis();
        log.info("start parse");
        parse(scrolls);
        long endTime = System.currentTimeMillis();
        log.info("end parse {} ms", endTime - startTime);
    }

    public void parse(int maxScrolls) {
        List<Smartphone> smartphones = new LinkedList<>();

        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(CARD_CSS)));

            List<WebElement> initialCards = driver.findElements(By.cssSelector(CARD_CSS));
            int loadedCount = initialCards.size();
            for (WebElement card : initialCards) {
                smartphones.add(parseCard(card));
            }

            boolean moreToLoad = true;
            int scrollCount = 0;

            while (moreToLoad && scrollCount < maxScrolls) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Parsing interrupted", e);
                    break;
                }

                List<WebElement> allCurrentCards = driver.findElements(By.cssSelector(CARD_CSS));
                int totalNow = allCurrentCards.size();

                if (totalNow > loadedCount) {
                    for (int i = loadedCount; i < totalNow; i++) {
                        WebElement card = allCurrentCards.get(i);
                        smartphones.add(parseCard(card));
                    }
                    loadedCount = totalNow;
                } else {
                    moreToLoad = false;
                }

                scrollCount++;
            }
        } finally {
            smartphoneRepository.saveAll(smartphones);
        }
    }

    private Smartphone parseCard(WebElement card) {
        Smartphone smartphone = new Smartphone();

        try {
            WebElement link = card.findElement(By.cssSelector(LINK_CSS));
            String href = link.getAttribute("href");
            String url = href.startsWith("http") ? href : "invalid url";
            String model = link.findElement(By.cssSelector(TITLE_CSS)).getText().trim();

            smartphone.setUrl(url);
            smartphone.setId(sep(url));
            smartphone.setModel(model);

            Map<String, Consumer<String>> specSetters = Map.of(
                    "диагональ экрана:", smartphone::setDiagonal,
                    "встроенная память :", smartphone::setMemory,
                    "оперативная память:", smartphone::setOpMemory,
                    "версия:", smartphone::setVersion,
                    "количество основных камер:", value -> {
                        try {
                            int cameraCount = Integer.parseInt(value);
                            smartphone.setCountCamera(cameraCount);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid camera count: {}", value);
                        }
                    }
            );

            List<WebElement> specs = card.findElements(By.cssSelector(SPEC_BLOCK_CSS));
            for (WebElement spec : specs) {
                String label = spec.findElement(By.cssSelector(SPEC_LABEL_CSS))
                        .getText().trim().toLowerCase();
                String value = spec.findElement(By.cssSelector(SPEC_VALUE_CSS))
                        .getText().trim().toLowerCase();

                Consumer<String> setter = specSetters.get(label);
                if (setter != null) {
                    setter.accept(value);
                } else {
                    log.info("Unknown specification label: {}", label);
                }
            }

            try {
                WebElement ratingBlock = card.findElement(By.cssSelector(RATING_BLOCK_CSS));
                String ratingValue = ratingBlock.findElement(By.cssSelector(RATING_VALUE_CSS))
                        .getText().trim();
                try {
                    double rating = Double.parseDouble(ratingValue);
                    smartphone.setRating(rating);
                } catch (NumberFormatException e) {
                    log.warn("Invalid rating: {}", ratingValue);
                }
            } catch (NoSuchElementException e) {
                log.info("No rating found for card");
            }
        } catch (Exception e) {
            log.error("Error parsing card: {}", e.getMessage(), e);
        }

        return smartphone;
    }

    private String sep(String s) {
        String start = "https://market.yandex.ru/card/";
        String start2 = "https://market.yandex.ru/product--";
        String url;
        if (s.startsWith(start)) {
            url = s.substring(start.length());
            String[] split = url.split("/");
            url = split[0] + split[1].split("[?]")[0];
        } else if (s.startsWith(start2)) {
            url = s.substring(start2.length());
            String[] split = url.split("/");
            url = split[0].replaceAll("-", "") + split[1].split("[?]")[0];
        } else {
            System.out.println(s);
            throw new FailureUrl(String.format("url must start with '%s' or '%s'", start, start2));
        }
        return url;
    }
}
