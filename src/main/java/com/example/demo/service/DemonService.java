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

import java.util.*;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@Component
@Slf4j
public class DemonService {

    private static final String CARD_SELECTOR = "div._1ENFO";
    private static final String LINK_SELECTOR = "a[data-auto='snippet-link']";
    private static final String TITLE_SELECTOR = "span[data-auto='snippet-title']";
    private static final String SPEC_BLOCK_SELECTOR = "div._2Ce4O";
    private static final String SPEC_LABEL_SELECTOR = "span.ds-text_color_text-secondary";
    private static final String SPEC_VALUE_SELECTOR = "span.ds-text_color_text-primary";
    private static final String RATING_SELECTOR = "span[data-auto='reviews']";
    private static final String RATING_VALUE_SELECTOR = "span.ds-rating__value";

    @Value("${parser.url.start}")
    private String startUrl;

    @Value("${parser.scrolls}")
    private int maxScrolls;

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final SmartphoneRepository repository;

    public DemonService(WebDriver driver, WebDriverWait wait, SmartphoneRepository repository) {
        this.driver = driver;
        this.wait = wait;
        this.repository = repository;
    }

    @Scheduled(fixedRateString = "${daemon.fixedRate:300000}")
    public void start() {
        long t0 = System.currentTimeMillis();
        log.info("Parsing started");
        List<Smartphone> smartphones = parseList(maxScrolls);
        repository.saveAll(smartphones);
        long t1 = System.currentTimeMillis();
        log.info("Parsing finished in {} ms", (t1 - t0));
    }

    private List<Smartphone> parseList(int scrollCountLimit) {
        List<Smartphone> result = new ArrayList<>();

        driver.get(startUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(CARD_SELECTOR)));

        List<WebElement> cards = driver.findElements(By.cssSelector(CARD_SELECTOR));
        int loaded = cards.size();
        cards.forEach(card -> result.add(parseCard(card)));

        for (int scrollIndex = 0; scrollIndex < scrollCountLimit; scrollIndex++) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted during scroll", e);
                break;
            }

            List<WebElement> allCards = driver.findElements(By.cssSelector(CARD_SELECTOR));
            int currentTotal = allCards.size();

            if (currentTotal <= loaded) {
                break;
            }

            for (int i = loaded; i < currentTotal; i++) {
                result.add(parseCard(allCards.get(i)));
            }
            loaded = currentTotal;
        }

        return result;
    }

    private Smartphone parseCard(WebElement card) {
        Smartphone phone = new Smartphone();

        try {
            WebElement link = card.findElement(By.cssSelector(LINK_SELECTOR));
            String href = link.getAttribute("href");
            String fullUrl = href.startsWith("http") ? href : "invalid url";
            phone.setUrl(fullUrl);
            phone.setId(extractId(fullUrl));
            phone.setModel(link.findElement(By.cssSelector(TITLE_SELECTOR)).getText().trim());

            Map<String, Consumer<String>> specMap = Map.of(
                    "диагональ экрана:", phone::setDiagonal,
                    "встроенная память :", phone::setMemory,
                    "оперативная память:", phone::setOpMemory,
                    "версия:", phone::setVersion,
                    "количество основных камер:", val -> {
                        try {
                            phone.setCountCamera(Integer.parseInt(val));
                        } catch (NumberFormatException ex) {
                            log.warn("Invalid camera count: {}", val);
                        }
                    }
            );

            card.findElements(By.cssSelector(SPEC_BLOCK_SELECTOR)).forEach(block -> {
                String label = block.findElement(By.cssSelector(SPEC_LABEL_SELECTOR))
                        .getText().trim().toLowerCase();
                String value = block.findElement(By.cssSelector(SPEC_VALUE_SELECTOR))
                        .getText().trim();
                Consumer<String> setter = specMap.get(label);
                if (setter != null) {
                    setter.accept(value);
                } else {
                    log.info("Unmapped spec label: {}", label);
                }
            });

            try {
                WebElement ratingBlock = card.findElement(By.cssSelector(RATING_SELECTOR));
                String ratingText = ratingBlock.findElement(By.cssSelector(RATING_VALUE_SELECTOR))
                        .getText().trim();
                phone.setRating(Double.parseDouble(ratingText));
            } catch (NoSuchElementException | NumberFormatException ex) {
                log.info("No valid rating for card or parse failed: {}", ex.getMessage());
            }
        } catch (Exception ex) {
            log.error("Failed to parse card: {}", ex.getMessage(), ex);
        }

        return phone;
    }

    private String extractId(String url) {
        final String prefixCard = "https://market.yandex.ru/card/";
        final String prefixProduct = "https://market.yandex.ru/product--";

        if (url.startsWith(prefixCard)) {
            String remainder = url.substring(prefixCard.length());
            String[] parts = remainder.split("/");
            return parts[0] + parts[1].split("\\?")[0];
        }

        if (url.startsWith(prefixProduct)) {
            String remainder = url.substring(prefixProduct.length());
            String[] parts = remainder.split("/");
            return parts[0].replaceAll("-", "") + parts[1].split("\\?")[0];
        }

        throw new FailureUrl("URL must start with '"
                + prefixCard + "' or '" + prefixProduct + "'");
    }
}
