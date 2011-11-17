package hudson.plugins.sauce_ondemand;

import com.saucelabs.rest.Credential;
import com.saucelabs.sauce_ondemand.driver.SauceOnDemandSelenium;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.junit.Assert;

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

        String h = build.getEnvironment(listener).get("SAUCE_ONDEMAND_HOST");
        if (h != null) System.setProperty("SAUCE_ONDEMAND_HOST", h);

        String url = build.getEnvironment(listener).get("SELENIUM_STARTING_URL");
        if (url == null) {
            url = "http://localhost:8080/";
        }
        System.setProperty("SELENIUM_STARTING_URL", url);

        System.setProperty("SELENIUM_DRIVER", Browser.Firefox3_0_Linux.getUri());

        Selenium selenium = SeleniumFactory.create();
        selenium.start();
        SauceOnDemandSelenium sauce = (SauceOnDemandSelenium) selenium;
        String sessionId = sauce.getSessionIdValue();

        try {
            selenium.open("/");
            // if the server really hit our Jetty, we should see the same title that includes the secret code.
            Assert.assertEquals("test" + secret, selenium.getTitle());

            if (testReport != null) {
                //create a test report for SauceOnDemandReportPublisher
                String xml = testReport.replace("SESSION_ID", sessionId);
                build.getWorkspace().child("test.xml").write(xml, "UTF-8");
            }

        } finally {
            selenium.stop();
        }

        return sessionId;
    }

}
