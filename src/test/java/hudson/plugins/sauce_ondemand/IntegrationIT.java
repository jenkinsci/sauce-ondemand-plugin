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

import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * Integration tests which use Selenium to performs a minimal set of validation to ensure that
 * the plugin is installed and integrates with Jenkins correctly.
 *
 * @author Ross Rowe
 */
public class IntegrationIT {

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
     * Creates a new {@link org.openqa.selenium.firefox.FirefoxDriver} instance, and instructs Firefox to open the URL associated with the
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
    public void pluginInstalled() {
        WebElement sauceStatus = webDriver.findElement(By.id("sauce_status"));
        assertNotNull("Status not found", sauceStatus);
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
