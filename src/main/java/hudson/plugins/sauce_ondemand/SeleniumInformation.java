package hudson.plugins.sauce_ondemand;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Contains information about the browser information selected in the Jenkins job configuration.
 *
 * @author Ross Rowe
 */
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
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
