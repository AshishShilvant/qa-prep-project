package com.qaprep.tests.ui;

import com.qaprep.framework.driver.DriverFactory;
import com.qaprep.framework.pages.LoginPage;
import com.qaprep.framework.pages.SecureAreaPage;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginTest {

    private static final String LOGIN_URL = "https://the-internet.herokuapp.com/login";

    @BeforeMethod
    public void openLoginPage() {
        DriverFactory.getDriver().get(LOGIN_URL);
    }

    @AfterMethod
    public void closeDriver() {
        DriverFactory.quitDriver();
    }

    @DataProvider(name = "loginData", parallel = true)
    public Object[][] loginData() {
        return new Object[][]{
                {"tomsmith", "SuperSecretPassword!", true, "You logged into a secure area!"},
                {"tomsmith", "wrongPassword", false, "Your password is invalid!"}
        };
    }

    @Test(dataProvider = "loginData")
    public void login(String username, String password, boolean expectSuccess, String expectedMessage) {
        WebDriver driver = DriverFactory.getDriver();
        LoginPage loginPage = new LoginPage(driver);

        if (expectSuccess) {
            SecureAreaPage secureAreaPage = loginPage.submitValidLogin(username, password);
            Assert.assertTrue(secureAreaPage.getFlashMessage().contains(expectedMessage));
        } else {
            LoginPage sameLoginPage = loginPage.submitInvalidLogin(username, password);
            Assert.assertTrue(sameLoginPage.getFlashMessage().contains(expectedMessage));
        }
    }
}
