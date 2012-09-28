package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author Ross Rowe
 */
public class SeleniumInformation implements Serializable {

    private String startingURL;

    private List<String> seleniumBrowsers;
    private List<String> webDriverBrowsers;
    private boolean isWebDriver;

    @DataBoundConstructor
    public SeleniumInformation(String value, String startingURL, List<String> seleniumBrowsers, List<String> webDriverBrowsers) {
        this.isWebDriver = value != null && value.equals("webDriver");
        this.startingURL = startingURL;
        this.seleniumBrowsers = seleniumBrowsers;
        this.webDriverBrowsers = webDriverBrowsers;
    }

    public String getStartingURL() {
        return startingURL;
    }

    public void setStartingURL(String startingURL) {
        this.startingURL = startingURL;
    }


    @Override
    public int hashCode() {
        int result = 17;
        if (startingURL != null) {
            result = 31 * result + startingURL.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SeleniumInformation)) {
            return false;
        }
        SeleniumInformation seleniumInformation = (SeleniumInformation) object;
        return (startingURL == null ? seleniumInformation.startingURL == null : startingURL.equals(seleniumInformation.startingURL));
    }

    @Override
    public String toString() {
        return startingURL == null ? super.toString() : startingURL;
    }

    public List<String> getSeleniumBrowsers() {
        return seleniumBrowsers;
    }

    public void setSeleniumBrowsers(List<String> seleniumBrowsers) {
        this.seleniumBrowsers = seleniumBrowsers;
    }

    public List<String> getWebDriverBrowsers() {
        return webDriverBrowsers;
    }

    public void setWebDriverBrowsers(List<String> webDriverBrowsers) {
        this.webDriverBrowsers = webDriverBrowsers;
    }

    public boolean isWebDriver() {
        return isWebDriver;
    }

    public void setWebDriver(boolean webDriver) {
        isWebDriver = webDriver;
    }
}
