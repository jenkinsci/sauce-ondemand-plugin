package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Contains information about the browser information selected in the Jenkins job configuration.
 *
 * @author Ross Rowe
 */
public class SeleniumInformation implements Serializable {

    private List<String> webDriverBrowsers;
    private List<String> appiumBrowsers;

    @DataBoundConstructor
    public SeleniumInformation(List<String> webDriverBrowsers, List<String> appiumBrowsers) {
        this.webDriverBrowsers = webDriverBrowsers;
        this.appiumBrowsers = appiumBrowsers;
    }

    public List<String> getWebDriverBrowsers() {
        return webDriverBrowsers;
    }

    public List<String> getAppiumBrowsers() {
        return appiumBrowsers;
    }

    public void setWebDriverBrowsers(List<String> webDriverBrowsers) {
        this.webDriverBrowsers = webDriverBrowsers;
    }

    public void setAppiumBrowsers(List<String> appiumBrowsers) {
        this.appiumBrowsers = appiumBrowsers;
    }
}
