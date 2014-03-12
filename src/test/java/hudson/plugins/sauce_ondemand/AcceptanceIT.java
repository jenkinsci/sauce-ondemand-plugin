package hudson.plugins.sauce_ondemand;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * Runs a series of tests using Selenium which verify that the plugin's functionality works in a 'live' environment.
 * <p/>
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
     * Closes the webDriver session when the test has finished.
     *
     * @throws Exception thrown if an unexpected error occurs
     */
    @After
    public void tearDown() throws Exception {
        webDriver.quit();
    }
}
