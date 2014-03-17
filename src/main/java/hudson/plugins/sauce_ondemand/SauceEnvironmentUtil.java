package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ross Rowe
 */
public class SauceEnvironmentUtil {

    private static final Logger logger = Logger.getLogger(SauceEnvironmentUtil.class.getName());
    public static final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

    private SauceEnvironmentUtil() {
    }


    public static void outputWebDriverVariables(Map<String, String> env, List<String> browsers, String userName, String apiKey) {

        if (browsers != null && !browsers.isEmpty()) {
            if (browsers.size() == 1) {
                Browser browserInstance = BROWSER_FACTORY.webDriverBrowserForKey(browsers.get(0));
                outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey);
            }

            JSONArray browsersJSON = new JSONArray();
            for (String browser : browsers) {
                Browser browserInstance = BROWSER_FACTORY.webDriverBrowserForKey(browser);
                browserAsJSON(browsersJSON, browserInstance, userName, apiKey);
                //output SELENIUM_DRIVER for the first browser so that the Selenium Client Factory picks up a valid uri pattern
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey));
            }
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SAUCE_ONDEMAND_BROWSERS, browsersJSON.toString());

        }
    }

    public static void outputSeleniumRCVariables(Map<String, String> env, List<String> browsers, String userName, String apiKey) {

        if (browsers != null && !browsers.isEmpty()) {
            if (browsers.size() == 1) {
                Browser browserInstance = BROWSER_FACTORY.seleniumBrowserForKey(browsers.get(0));
                outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey);
            }

            JSONArray browsersJSON = new JSONArray();
            for (String browser : browsers) {
                Browser browserInstance = BROWSER_FACTORY.seleniumBrowserForKey(browser);
                browserAsJSON(browsersJSON, browserInstance, userName, apiKey);
                //output SELENIUM_DRIVER for the first browser so that the Selenium Client Factory picks up a valid uri pattern
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey));
            }
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SAUCE_ONDEMAND_BROWSERS, browsersJSON.toString());

        }
    }

    public static void outputAppiumVariables(Map<String, String> env, List<String> browsers, String userName, String apiKey) {

        if (browsers != null && !browsers.isEmpty()) {
            if (browsers.size() == 1) {
                Browser browserInstance = BROWSER_FACTORY.appiumBrowserForKey(browsers.get(0));
                outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey);
            }

            JSONArray browsersJSON = new JSONArray();
            for (String browser : browsers) {
                Browser browserInstance = BROWSER_FACTORY.appiumBrowserForKey(browser);
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
            if (browserInstance.getDevice() != null) {
                config.put("device", browserInstance.getDevice());
            }
            if (browserInstance.getDeviceType() != null) {
                config.put("device-type", browserInstance.getDeviceType());
            }
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

            if (browserInstance.getDevice() != null) {
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DEVICE, browserInstance.getDevice(), overwrite);
            }
            if (browserInstance.getDeviceType() != null) {
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DEVICE_TYPE, browserInstance.getDeviceType(), overwrite);
            }
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

    public static String getBuildName(AbstractBuild<?, ?> build) {
        String displayName = build.getFullDisplayName();
        String buildName = build.getDisplayName();
        StringBuilder builder = new StringBuilder(displayName);
        //for multi-config projects, the full display name contains the build name twice
        //detect this and replace the second occurance with the build number
        if (StringUtils.countMatches(displayName, buildName) > 1) {
            builder.replace(displayName.lastIndexOf(buildName), displayName.length(), "#" + build.getNumber());
        }
        return builder.toString();
    }
}
