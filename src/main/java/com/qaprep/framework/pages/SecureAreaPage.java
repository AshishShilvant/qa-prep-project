package com.qaprep.framework.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * https://the-internet.herokuapp.com/secure — landed on after a successful login.
 */
public class SecureAreaPage extends BasePage {

    @FindBy(css = "#content h2")
    private WebElement heading;

    @FindBy(id = "flash")
    private WebElement flashMessage;

    @FindBy(css = "a.button.secondary.radius")
    private WebElement logoutButton;

    public SecureAreaPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public String getHeadingText() {
        return waitVisible(heading).getText();
    }

    public String getFlashMessage() {
        return waitVisible(flashMessage).getText();
    }

    public LoginPage logout() {
        waitVisible(logoutButton).click();
        return new LoginPage(driver);
    }
}
