package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import hudson.maven.MavenBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Run;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains helper methods.
 *
 * @author Ross Rowe
 */
public final class SauceEnvironmentUtil {

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceEnvironmentUtil.class.getName());

    //only allow word, digit, and hyphen characters
    private static final String PATTERN_DISALLOWED_CHARS = "[^\\w\\d-]+";

    /**
     * Disallow instantiation of class.
     */
    private SauceEnvironmentUtil() {
    }

    /**
     * Adds the environment variables for the selected Appium browsers.
     *
     * @param env      the map of environment variables
     * @param browsers the list of selected browsers
     * @param userName the Sauce user name
     * @param apiKey   the Sauce access key
     * @param verboseLogging    Enable/Disable verbose logging of env variables
     * @param logger Where to log if necessary
     */
    public static void outputVariables(Map<String, String> env, List<Browser> browsers, String userName, String apiKey, boolean verboseLogging, PrintStream logger) {

        if (browsers != null && !browsers.isEmpty()) {
            if (browsers.size() == 1) {
                Browser browserInstance = browsers.get(0);
                outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey, verboseLogging, logger);
            }

            JSONArray browsersJSON = new JSONArray();
            for (Browser browserInstance : browsers) {

                browserAsJSON(browsersJSON, browserInstance, userName, apiKey);
                //output SELENIUM_DRIVER for the first browser so that the Selenium Client Factory picks up a valid uri pattern
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey), verboseLogging, logger);
            }
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SAUCE_ONDEMAND_BROWSERS, browsersJSON.toString(), verboseLogging, logger);

        }
    }

    /**
     * Populates the JSONArray with a JSON representation of the selected browser
     *
     * @param browsersJSON    array of browsers
     * @param browserInstance selected Browser being processed
     * @param userName        the Sauce username
     * @param apiKey          the Sauce access key
     */
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
            config.put("long-name", browserInstance.getLongName());
            config.put("long-version", browserInstance.getLongVersion());
            config.put("url", browserInstance.getUri(userName, apiKey));
            if (browserInstance.getDevice() != null) {
                config.put("device", browserInstance.getDevice());
            }
            if (browserInstance.getDeviceType() != null) {
                config.put("device-type", browserInstance.getDeviceType());
            }
            if (browserInstance.getDeviceOrientation() != null) {
                config.put("device-orientation", browserInstance.getDeviceOrientation());
            }
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Unable to create JSON Object", e);
        }
        browsersJSON.add(config);
    }

    /**
     * Adds the environment variables for the selected browser.
     *
     * @param env      the map of environment variables
     * @param browserInstance   FIXME
     * @param userName the Sauce user name
     * @param apiKey   the Sauce access key
     * @param verboseLogging    Enable/Disable verbose logging of env variables
     * @param printStream Where to log if necessary
     */
    public static void outputEnvironmentVariablesForBrowser(Map<String, String> env, Browser browserInstance, String userName, String apiKey, boolean verboseLogging, PrintStream printStream) {
        outputEnvironmentVariablesForBrowser(env, browserInstance, userName, apiKey, false, verboseLogging, printStream);
    }

    /**
     * Adds the environment variables for the selected browser.
     *
     * @param env       the map of environment variables
     * @param browserInstance    FIXME
     * @param userName  the Sauce user name
     * @param apiKey    the Sauce access key
     * @param overwrite indicates whether existing environment variables should be overwritten
     * @param verboseLogging    Enable/Disable verbose logging of env variables
     * @param printStream Where to log if necessary
     */
    public static void outputEnvironmentVariablesForBrowser(Map<String, String> env, Browser browserInstance, String userName, String apiKey, boolean overwrite, boolean verboseLogging, PrintStream printStream) {

        if (browserInstance != null) {

            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_PLATFORM, browserInstance.getOs(), overwrite, verboseLogging, printStream);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_BROWSER, browserInstance.getBrowserName(), overwrite, verboseLogging, printStream);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_VERSION, browserInstance.getVersion(), overwrite, verboseLogging, printStream);
            outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DRIVER, browserInstance.getUri(userName, apiKey), overwrite, verboseLogging, printStream);

            if (browserInstance.getDevice() != null) {
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DEVICE, browserInstance.getDevice(), overwrite, verboseLogging, printStream);
            }
            if (browserInstance.getDeviceType() != null) {
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DEVICE_TYPE, browserInstance.getDeviceType(), overwrite, verboseLogging, printStream);
            }
            if (browserInstance.getDeviceOrientation() != null) {
                outputEnvironmentVariable(env, SauceOnDemandBuildWrapper.SELENIUM_DEVICE_ORIENTATION, browserInstance.getDeviceOrientation(), overwrite, verboseLogging, printStream);
            }
        }
    }

    /**
     * Adds the key/value pair to the map of environment variables.
     *
     * @param env   the map of environment variables
     * @param key   environment variable key
     * @param value environment variable value
     * @param verboseLogging    Enable/Disable verbose logging of env variables
     * @param printStream Where to log if necessary
     */
    public static void outputEnvironmentVariable(Map<String, String> env, String key, String value, boolean verboseLogging, PrintStream printStream) {
        outputEnvironmentVariable(env, key, value, false, verboseLogging, printStream);
    }

    /**
     * Adds the key/value pair to the map of environment variables.
     *
     * @param env       the map of environment variables
     * @param key       environment variable key
     * @param value     environment variable value
     * @param overwrite indicates whether existing environment variables should be overwritten
     * @param verboseLogging    Enable/Disable verbose logging of env variables
     * @param printStream Where to log if necessary
     */
    public static void outputEnvironmentVariable(Map<String, String> env, String key, String value, boolean overwrite, boolean verboseLogging, PrintStream printStream) {
        if (env.get(key) == null || overwrite) {
            PluginImpl pluginImpl = PluginImpl.get();
            String environmentVariablePrefix = "";
            if (pluginImpl != null) {
                environmentVariablePrefix = pluginImpl.getEnvironmentVariablePrefix();
                if (environmentVariablePrefix == null) {
                    environmentVariablePrefix = "";
                }
            }
            env.put(environmentVariablePrefix + key, value);
            if (verboseLogging)
            {
                printStream.println(environmentVariablePrefix + key + ": " + value);
            }
        }
    }

    /**
     * @param project the Jenkins project to check
     * @return the SauceOnDemandBuildWrapper instance associated with the project, can be null
     */
    public static SauceOnDemandBuildWrapper getBuildWrapper(AbstractProject<?, ?> project) {
        SauceOnDemandBuildWrapper buildWrapper = null;
        if (project instanceof BuildableItemWithBuildWrappers) {
            buildWrapper = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList().get(SauceOnDemandBuildWrapper.class);
        } else {
            logger.fine("Project is not a BuildableItemWithBuildWrappers instance " + project.toString());
        }
        if (buildWrapper == null) {
            logger.fine("Could not find SauceOnDemandBuildWrapper on project " + project.toString());
        }
        return buildWrapper;
    }

    /**
     *
     * @param build the Jenkins build
     * @return String representing the Jenkins build
     */
    @Nonnull
    public static String getBuildName(Run<?, ?> build) {
        while (build != null && build instanceof MavenBuild && ((MavenBuild) build).getParentBuild() != null) {
            build = ((MavenBuild) build).getParentBuild();
        }
        if (build == null) { return ""; }

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

    public static String generateTunnelName(final String projectName) {
        //String rawName = build.getProject().getName();
        String sanitizedName = projectName.replaceAll(PATTERN_DISALLOWED_CHARS, "_");
        return sanitizedName + "-" + System.currentTimeMillis();
    }

    // the buildNumber variable returned from the API uses hyphens, so we should sanitize with hyphens here
    public static String getSanitizedBuildNumber(Run run) {
        return "jenkins-"+SauceEnvironmentUtil.getBuildName(run).replaceAll(PATTERN_DISALLOWED_CHARS, "-");
    }

}
