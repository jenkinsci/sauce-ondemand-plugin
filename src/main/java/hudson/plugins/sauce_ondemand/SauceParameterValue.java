package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import net.sf.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Ross Rowe
 */
public class SauceParameterValue extends ParameterValue {

    /** Handles the retrieval of browsers from Sauce Labs. */
    private static final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

    private final JSONArray selectedBrowsers;

    @DataBoundConstructor
    public SauceParameterValue(String name, JSONArray selectedBrowsers) {
        super(name);
        this.selectedBrowsers = selectedBrowsers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        SauceCredentials credentials = SauceCredentials.getCredentials(build);
        String userName = credentials.getUsername();
        String apiKey = credentials.getPassword().getPlainText();

        if (selectedBrowsers != null && !selectedBrowsers.isEmpty()) {
            if (selectedBrowsers.size() == 1) {
                Browser browserInstance = BROWSER_FACTORY.webDriverBrowserForKey(selectedBrowsers.getString(0));
                SauceEnvironmentUtil.outputEnvironmentVariablesForBrowser(
                    env,
                    browserInstance,
                    userName,
                    apiKey,
                    true,
                    false,
                    null
                );
            } else {
                JSONArray browsersJSON = new JSONArray();
                for (int i = 0; i < selectedBrowsers.size(); i++) {
                    String browser = selectedBrowsers.getString(i);
                    {
                        Browser browserInstance = BrowserFactory.getInstance().webDriverBrowserForKey(browser);
                        SauceEnvironmentUtil.browserAsJSON(browsersJSON, browserInstance, userName, apiKey);
                        //output SELENIUM_DRIVER for the first browser so that the Selenium Client Factory picks up a valid uri pattern
                        SauceEnvironmentUtil.outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey), true, false, null);
                    }
                    SauceEnvironmentUtil.outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SAUCE_ONDEMAND_BROWSERS, browsersJSON.toString(), true, false, null);

                }
            }
        }

    }
}
