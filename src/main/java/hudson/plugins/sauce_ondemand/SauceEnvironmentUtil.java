package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ross Rowe
 */
public class SauceEnvironmentUtil {

    private static final Logger logger = Logger.getLogger(SauceEnvironmentUtil.class.getName());

    private SauceEnvironmentUtil() {
    }


    public static void outputWebDriverVariables(Map<String, String> env, List<String> browsers, String userName, String apiKey) {

        if (browsers != null && !browsers.isEmpty()) {
            if (browsers.size() == 1) {
                Browser browserInstance = BrowserFactory.getInstance().webDriverBrowserForKey(browsers.get(0));
                outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey);
            }

            JSONArray browsersJSON = new JSONArray();
            for (String browser : browsers) {
                Browser browserInstance = BrowserFactory.getInstance().webDriverBrowserForKey(browser);
                browserAsJSON(browsersJSON, browserInstance, userName, apiKey);
                //output SELENIUM_DRIVER for the first browser so that the Selenium Client Factory picks up a valid uri pattern
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey));
            }
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SAUCE_ONDEMAND_BROWSERS, browsersJSON.toString());

        }
    }


    public static void browserAsJSON(JSONArray browsersJSON, Browser browserInstance, String userName, String apiKey) {
        if (browserInstance == null) {
            return;
        }
        JSONObject config = new JSONObject();
        try {
            config.put("os", browserInstance.getOs());
            config.put("platform", browserInstance.getPlatform().toString());
            config.put("browser", browserInstance.getBrowserName());
            config.put("browser-version", browserInstance.getVersion());
            config.put("url", browserInstance.getUri(userName, apiKey));
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Unable to create JSON Object", e);
        }
        browsersJSON.add(config);
    }

    public static void outputEnvironmentVariablesForBrowser(Map<String, String> env, Browser browserInstance, String userName, String apiKey) {
        outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey, false);
    }

    public static void outputEnvironmentVariablesForBrowser(Map<String, String> env, Browser browserInstance, String userName, String apiKey, boolean overwrite) {

        if (browserInstance != null) {

            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_PLATFORM, browserInstance.getOs(), overwrite);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_BROWSER, browserInstance.getBrowserName(), overwrite);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_VERSION, browserInstance.getVersion(), overwrite);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey), overwrite);
        }
    }

    public static void outputEnvironmentVariable(Map<String, String> env, String key, String value) {
        outputEnvironmentVariable(env, key, value, false);
    }

    public static void outputEnvironmentVariable(Map<String, String> env, String key, String value, boolean overwrite) {
        if (env.get(key) == null || overwrite) {
            env.put(key, value);
        }
    }


    public static SauceOnDemandBuildWrapper getBuildWrapper(AbstractProject<?, ?> project) {
        SauceOnDemandBuildWrapper buildWrapper = null;
        if (project instanceof BuildableItemWithBuildWrappers) {
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList();
            for (BuildWrapper describable : buildWrappers) {
                if (describable instanceof SauceOnDemandBuildWrapper) {
                    buildWrapper = (SauceOnDemandBuildWrapper) describable;
                    break;
                }
            }
        } else {
            logger.info("Project is not a BuildableItemWithBuildWrappers instance " + project.toString());
        }
        if (buildWrapper == null) {
            logger.info("Could not find SauceOnDemandBuildWrapper on project " + project.toString());
        }
        return buildWrapper;
    }
}
