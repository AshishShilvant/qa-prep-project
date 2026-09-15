package com.qaprep.framework.pages;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.time.Duration;

/**
 * Common base for all page objects. Holds the thread's WebDriver and the
 * shared explicit-wait helpers that page objects build on.
 */
public abstract class BasePage {

    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Waits until the given element is visible (present in the DOM and displayed).
     * FluentWait: 10s timeout, polled every 300ms, transient NoSuchElementException
     * swallowed between polls.
     *
     * @return the same element, so calls can be chained
     */
    protected WebElement waitVisible(WebElement element) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(NoSuchElementException.class);

        return wait.until(ExpectedConditions.visibilityOf(element));
    }
}
