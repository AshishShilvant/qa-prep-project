package com.qaprep.framework.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * https://the-internet.herokuapp.com/login
 */
public class LoginPage extends BasePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(id = "flash")
    private WebElement flashMessage;

    public LoginPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public LoginPage enterUsername(String username) {
        waitVisible(usernameInput).sendKeys(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        waitVisible(passwordInput).sendKeys(password);
        return this;
    }

    /**
     * Submits the form with credentials expected to succeed, landing on the secure area.
     */
    public SecureAreaPage submitValidLogin(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        waitVisible(submitButton).click();
        return new SecureAreaPage(driver);
    }

    /**
     * Submits the form with credentials expected to fail; the app re-renders this same
     * page with a flash error, so the returned type stays LoginPage.
     */
    public LoginPage submitInvalidLogin(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        waitVisible(submitButton).click();
        return this;
    }

    public String getFlashMessage() {
        return waitVisible(flashMessage).getText();
    }
}
