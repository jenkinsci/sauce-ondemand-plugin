package hudson.plugins.sauce_ondemand;

import com.saucelabs.rest.Credential;
import com.saucelabs.sauce_ondemand.driver.SauceOnDemandSelenium;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 20/02/11
 * Time: 10:06
 * To change this template use File | Settings | File Templates.
 */
public class TeztSimulation {

    private AbstractBuild<?, ?> build;
    private BuildListener listener;
    private int secret;
    private String testReport;
    public static final String JOB_NAME = TeztSimulation.class.getName() + ".doTest";
    public static final String URL = "/testReport/hudson.plugins.sauce_ondemand/TeztSimulation/doTest";

    TeztSimulation(AbstractBuild<?, ?> build, BuildListener listener, int secret, String testReport) {

        this.build = build;
        this.listener = listener;
        this.secret = secret;
        this.testReport = testReport;
    }

    public String doTest() throws IOException, InterruptedException {
        Credential c = new Credential();

        EnvVars envVars = build.getEnvironment(listener);
        String h = envVars.get("SELENIUM_HOST");
        if (h != null) System.setProperty("SELENIUM_HOST", h);

        String port = envVars.get("SELENIUM_PORT");
        if (port != null) System.setProperty("SELENIUM_PORT", port);

        String url = envVars.get("SELENIUM_STARTING_URL");
        if (url == null) {
            url = "http://localhost:8080/";
        }
        System.setProperty("SELENIUM_STARTING_URL", url);

        System.setProperty("SELENIUM_DRIVER", new com.saucelabs.ci.Browser("Chrome", "linux", "firefox", "4.0", "firefox").getUri());

        WebDriver selenium = SeleniumFactory.createWebDriver();

        SauceOnDemandSelenium sauceOnDemandSelenium = (SauceOnDemandSelenium) selenium;
        String sessionId = sauceOnDemandSelenium.getSessionIdValue();


        try {
            selenium.get("http://localhost:8080/");
            // if the server really hit our Jetty, we should see the same title that includes the secret code.
            Assert.assertEquals("test" + secret, selenium.getTitle());

            if (testReport != null) {
                //create a test report for SauceOnDemandReportPublisher
                String xml = testReport.replace("SESSION_ID", sessionId);
                build.getWorkspace().child("test.xml").write(xml, "UTF-8");
            }

        } finally {
            selenium.quit();
        }

        return sessionId;
    }

}
