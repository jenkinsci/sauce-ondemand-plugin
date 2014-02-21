package hudson.plugins.sauce_ondemand;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Runs a series of tests using Selenium which verify that the plugin's functionality works in a 'live' environment.
 *
 * For ease of demonstration, these tests use the local Jenkins instance supplied by the {@link JenkinsRule},
 * but these tests could instead be configured to run against a live Jenkins environment by changing the url
 * referenced by the {@link #webDriver} instance.
 *
 * @author Ross Rowe
 */
public class AcceptanceIT {

    /**
     * WebDriver instance which will be used to perform browser interactions.
     */
    private WebDriver webDriver;

    /**
     * JUnit rule which instantiates a local Jenkins instance with our plugin installed.
     */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Creates a new {@link FirefoxDriver} instance, and instructs Firefox to open the URL associated with the
     * local Jenkins instance.
     *
     * @throws Exception thrown if an unexpected error occurs
     */
    @Before
    public void setUp() throws Exception {
        webDriver = new FirefoxDriver();
        URL url = jenkinsRule.getURL();
        webDriver.get(url.toString());
    }

    /**
     * Verifies that the plugin has been installed correctly.
     */
    @Test
    public void statusIsOkay() {
        WebElement sauceStatus = webDriver.findElement(By.id("sauce_status"));

        assertNotNull("Status not found", sauceStatus);
        WebElement sauceStatusMessage = webDriver.findElement(By.id("sauce_status_msg"));
        assertEquals("Status text not expected", "Basic service status checks passed.", sauceStatusMessage.getText());
    }

    /**
     * Click links to verify that the custom footer displays on each page.  This test also
     * demonstrates the various mechanisms you can use to select elements on page, using
     * the link text, element id, xpath and css selectors.
     */
    @Test
    public void navigation() {



        //Click the 'New Job' link using the link text as a selector
        webDriver.findElement(By.linkText("New Job")).click();
        assertNotNull("Status not found", webDriver.findElement(By.id("sauce_status")));


    }

    /**
     * Verifies the colour of the status element.
     */
    @Test
    public void colourOfStatus() {
        WebElement sauceStatus = webDriver.findElement(By.className("sauce_up"));
        assertNotNull("Status not found", sauceStatus);
        String colour = sauceStatus.getCssValue("color");
        assertEquals("Colour not green", "rgba(0, 128, 0, 1)", colour);
    }

    /**
     * Verifies the behaviour of clicking on the 'Check Now' link, which performs an Ajax call to re-query the Sauce status.
     */
    @Test
    public void ajaxAction() {
        WebElement sauceStatusProgressImage = webDriver.findElement(By.id("sauce_status_progress"));
        WebElement sauceStatusMessage = webDriver.findElement(By.id("sauce_status_msg"));
        assertFalse("Element is visible", sauceStatusProgressImage.isDisplayed());
        WebElement checkNowLink = webDriver.findElement(By.id("sauce_check_status_now"));
        assertNotNull("Status not found", checkNowLink);
        //click the link
        checkNowLink.click();
        //verify that the loading image is displayed and the status has changed to Checking...
        assertTrue("Element is not visible", sauceStatusProgressImage.isDisplayed());
        assertEquals("Status text not 'Checking'", "Checking...", sauceStatusMessage.getText());

        //wait until the status
        WebDriverWait wait = new WebDriverWait(webDriver, 30);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("sauce_status_progress")));
        assertEquals("Status text not expected", "Basic service status checks passed.", sauceStatusMessage.getText());
    }

    /**
     * Closes the webDriver session when the test has finished.
     *
     * @throws Exception thrown if an unexpected error occurs
     */
    @After
    public void tearDown() throws Exception {
        webDriver.quit();
    }
}
