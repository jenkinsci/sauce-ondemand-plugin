/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.sauce_ondemand;

import com.michelin.cio.hudson.plugins.copytoslave.MyFilePath;
import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.ci.sauceconnect.AbstractSauceTunnelManager;
import com.saucelabs.ci.sauceconnect.SauceConnectUtils;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.hudson.HudsonSauceConnectFourManager;
import com.saucelabs.hudson.HudsonSauceManagerFactory;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.LineTransformationOutputStream;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.tasks.BuildWrapper;
import hudson.util.Secret;
import hudson.util.VariableResolver;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel and populates environment variables which
 * represent the selected browser(s).
 *
 * @author Kohsuke Kawaguchi
 * @author Ross Rowe
 */
public class SauceOnDemandBuildWrapper extends BuildWrapper implements Serializable {

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildWrapper.class.getName());
    /** Environment variable key which contains Selenium Client Factory driver for selected browser. */
    public static final String SELENIUM_DRIVER = "SELENIUM_DRIVER";
    /** Environment variable key which contains a JSON formatted list of selected browsers. */
    public static final String SAUCE_ONDEMAND_BROWSERS = "SAUCE_ONDEMAND_BROWSERS";
    /** Environment variable key which contains the selenium host. */
    public static final String SELENIUM_HOST = "SELENIUM_HOST";
    /** Environment variable key which contains the selenium port. */
    public static final String SELENIUM_PORT = "SELENIUM_PORT";
    /** Environment variable key which contains the Sauce user name.*/
    private static final String SAUCE_USERNAME = "SAUCE_USER_NAME";
    /** Environment variable key which contains the Sauce access key.*/
    private static final String SAUCE_API_KEY = "SAUCE_API_KEY";
    /** Environment variable key which contains the device value for the selected browser.*/
    public static final String SELENIUM_DEVICE = "SELENIUM_DEVICE";
    /** Environment variable key which contains the device type for the selected browser.*/
    public static final String SELENIUM_DEVICE_TYPE = "SELENIUM_DEVICE_TYPE";
    /** Environment variable key which contains the device orientation for the selected browser.*/
    public static final String SELENIUM_DEVICE_ORIENTATION = "SELENIUM_DEVICE_ORIENTATION";
    /** Regex pattern which is used to identify replacement parameters. */
    public static final Pattern ENVIRONMENT_VARIABLE_PATTERN = Pattern.compile("[$|%]([a-zA-Z_][a-zA-Z0-9_]+)");
    /** Environment variable key which contains the browser value for the selected browser.*/
    public static final String SELENIUM_BROWSER = "SELENIUM_BROWSER";
    /** Environment variable key which contains the platform for the selected browser.*/
    public static final String SELENIUM_PLATFORM = "SELENIUM_PLATFORM";
    /** Environment variable key which contains the version for the selected browser.*/
    public static final String SELENIUM_VERSION = "SELENIUM_VERSION";
    /** Environment variable key which contains the Jenkins build number.*/
    private static final String JENKINS_BUILD_NUMBER = "JENKINS_BUILD_NUMBER";

    private static final long serialVersionUID = 1L;
    /**
     * The starting url to be used for tests run by the build.
     */
    private String startingURL;
    /**
     * The path to an existing Sauce Connect binary.
     */
    private String sauceConnectPath;
    /**
     * Indicates whether Sauce Connect v3 should be used.
     */
    private boolean useOldSauceConnect;
    /**
     * Indicates whether Sauce Connect should be started as part of the build.
     */
    private boolean enableSauceConnect;
    /**
     * Map of log parser instances, keyed on the Jenkins build number.
     */
    private Map<String, SauceOnDemandLogParser> logParserMap;
    /**
     * Host location of the selenium server.
     */
    private String seleniumHost;
    /**
     * Port location of the selenium server.
     */
    private String seleniumPort;
    /**
     * The Sauce username/access key that should be used for the build.
     */
    private Credentials credentials;
    /**
     * The browser information that is to be used for the build.
     */
    private SeleniumInformation seleniumInformation;
    /**
     * The list of selected WebDriver browsers.
     */
    private List<String> webDriverBrowsers;
    /**
     * The list of selected Appium browsers.
     */
    private List<String> appiumBrowsers;
    /**
     * Boolean which indicates whether the latest available version of the browser should be used.
     */
    private boolean useLatestVersion;
    /**
     * Default behaviour is to launch Sauce Connect on the slave node.
     */
    private boolean launchSauceConnectOnSlave = true;
    /**
     * String representing the HTTPS protocol to be used.
     */
    private String httpsProtocol;
    /**
     * The Sauce Connect command line options to be used.
     */
    private String options;
    /**
     * Default verbose logging to true.
     */
    private boolean verboseLogging = true;
    /**
     * RunCondition which allows users to define rules which enable Sauce Connect.
     */
    private RunCondition condition;


    /**
     * Constructs a new instance using data entered on the job configuration screen.
     *
     * @param enableSauceConnect        indicates whether Sauce Connect should be started as part of the build.
     * @param condition                 allows users to define rules which enable Sauce Connect
     * @param credentials               the Sauce username/access key that should be used for the build.
     * @param seleniumInformation       the browser information that is to be used for the build.
     * @param seleniumHost              host location of the selenium server.
     * @param seleniumPort              port location of the selenium server.
     * @param httpsProtocol             string representing the HTTPS protocol to be used.
     * @param options                   the Sauce Connect command line options to be used
     * @param startingURL               the starting url to be used for tests run by the build.
     * @param launchSauceConnectOnSlave indicates whether Sauce Connect should be launched on the slave or master node
     * @param useOldSauceConnect        indicates whether Sauce Connect 3 should be launched
     * @param verboseLogging            indicates whether the Sauce Connect output should be written to the Jenkins job output
     * @param useLatestVersion          indicates whether the latest version of the selected browser(s) should be used
     */
    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(
            boolean enableSauceConnect,
            RunCondition condition,
            Credentials credentials,
            SeleniumInformation seleniumInformation,
            String seleniumHost,
            String seleniumPort,
            String httpsProtocol,
            String options,
            String startingURL,
            String sauceConnectPath,
            boolean launchSauceConnectOnSlave,
            boolean useOldSauceConnect,
            boolean verboseLogging,
            boolean useLatestVersion
    ) {
        this.credentials = credentials;
        this.seleniumInformation = seleniumInformation;
        this.enableSauceConnect = enableSauceConnect;
        this.seleniumHost = seleniumHost;
        this.seleniumPort = seleniumPort;
        this.httpsProtocol = httpsProtocol;
        this.options = options;
        this.startingURL = startingURL;
        if (seleniumInformation != null) {
            this.webDriverBrowsers = seleniumInformation.getWebDriverBrowsers();
            this.appiumBrowsers = seleniumInformation.getAppiumBrowsers();
        }
        this.launchSauceConnectOnSlave = launchSauceConnectOnSlave;
        this.useOldSauceConnect = useOldSauceConnect;
        this.verboseLogging = verboseLogging;
        this.useLatestVersion = useLatestVersion;
        this.condition = condition;
        this.sauceConnectPath = sauceConnectPath;
    }


    /**
     * {@inheritDoc}
     * <p/>
     * Invoked prior to the running of a Jenkins build.  Populates the Sauce specific environment variables and launches Sauce Connect.
     *
     * @return a new {@link hudson.model.Environment} instance populated with the Sauce environment variables
     */
    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting pre-build for Sauce Labs plugin");
        logger.fine("Setting up Sauce Build Wrapper");
        if (isEnableSauceConnect()) {

            boolean canRun = true;
            try {
                if (condition != null) {
                    canRun = condition.runPerform(build, listener);
                }
            } catch (Exception e) {
                listener.getLogger().println("Error checking Sauce Connect run condition");
                throw new IOException(e);
            }
            if (canRun) {
                String workingDirectory = PluginImpl.get().getSauceConnectDirectory();
                String resolvedOptions = getCommandLineOptions(build, listener);
                if (launchSauceConnectOnSlave) {
                    listener.getLogger().println("Starting Sauce Connect on slave node using tunnel identifier: " + AbstractSauceTunnelManager.getTunnelIdentifier(resolvedOptions, "default"));

                    if (useOldSauceConnect && !(Computer.currentComputer() instanceof Hudson.MasterComputer)) {
                        //only copy sauce connect jar if we are using Sauce Connect v3
                        File sauceConnectJar = copySauceConnectToSlave(build, listener);
                        Computer.currentComputer().getChannel().call(
                                new SauceConnectHandler(
                                        this,
                                        listener,
                                        workingDirectory,
                                        resolvedOptions,
                                        sauceConnectJar));
                    } else {
                        Computer.currentComputer().getChannel().call
                                (new SauceConnectHandler(
                                        this,
                                        listener,
                                        workingDirectory,
                                        resolvedOptions
                                ));
                    }
                } else {
                    listener.getLogger().println("Starting Sauce Connect on master node using identifier: " + AbstractSauceTunnelManager.getTunnelIdentifier(resolvedOptions, "default"));
                    //launch Sauce Connect on the master
                    SauceConnectHandler sauceConnectStarter = new SauceConnectHandler(this, listener, workingDirectory, resolvedOptions);
                    sauceConnectStarter.call();

                }
            } else {
                listener.getLogger().println("Sauce Connect launch skipped due to run condition");
            }
        }
        listener.getLogger().println("Finished pre-build for Sauce Labs plugin");

        return new Environment() {

            /**
             * Updates the environment variable map to include the Sauce specific environment variables applicable to the build.
             * @param env existing environment variables
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                logger.fine("Creating Sauce environment variables");
                SauceEnvironmentUtil.outputWebDriverVariables(env, webDriverBrowsers, getUserName(), getApiKey(), isUseLatestVersion());
                SauceEnvironmentUtil.outputAppiumVariables(env, appiumBrowsers, getUserName(), getApiKey());
                //if any variables have been defined in build variables (ie. by a multi-config project), use them
                Map buildVariables = build.getBuildVariables();
                String environmentVariablePrefix = PluginImpl.get().getEnvironmentVariablePrefix();
                if (environmentVariablePrefix == null) {
                    environmentVariablePrefix = "";
                }

                if (buildVariables.containsKey(SELENIUM_BROWSER)) {
                    env.put(environmentVariablePrefix + SELENIUM_BROWSER, (String) buildVariables.get(SELENIUM_BROWSER));
                }
                if (buildVariables.containsKey(SELENIUM_VERSION)) {
                    env.put(environmentVariablePrefix + SELENIUM_VERSION, (String) buildVariables.get(SELENIUM_VERSION));
                }
                if (buildVariables.containsKey(SELENIUM_PLATFORM)) {
                    env.put(environmentVariablePrefix + SELENIUM_PLATFORM, (String) buildVariables.get(SELENIUM_PLATFORM));
                }
                env.put(JENKINS_BUILD_NUMBER, sanitiseBuildNumber(build.toString()));
                env.put(SAUCE_USERNAME, getUserName());
                env.put(SAUCE_API_KEY, getApiKey());
                env.put(SELENIUM_HOST, getHostName());

                DecimalFormat myFormatter = new DecimalFormat("####");
                env.put(SELENIUM_PORT, myFormatter.format(getPort()));

            }

            /**
             * @return the host name to be used for the build.
             */
            private String getHostName() {
                if (StringUtils.isNotBlank(seleniumHost)) {
                    Matcher matcher = ENVIRONMENT_VARIABLE_PATTERN.matcher(seleniumHost);
                    if (matcher.matches()) {
                        String variableName = matcher.group(1);
                        return System.getenv(variableName);
                    }
                    return seleniumHost;
                } else {
                    if (isEnableSauceConnect()) {
                        return getCurrentHostName();
                    } else {
                        return "ondemand.saucelabs.com";
                    }
                }
            }

            /**
             * {@inheritDoc}
             *
             * Terminates the Sauce Connect process (if the build is configured to launch Sauce Connect), and adds a {@link SauceOnDemandBuildAction} instance to the build.
             *  @param build
             *      The build in progress for which an {@link Environment} object is created.
             *      Never null.
             * @param listener
             *      Can be used to send any message.
             *
             */
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                listener.getLogger().println("Starting post-build for Sauce Labs plugin");
                if (isEnableSauceConnect()) {
                    boolean shouldClose = true;
                    try {
                        if (condition != null) {
                            shouldClose = condition.runPerform(build, listener);
                        }
                    } catch (Exception e) {
                        listener.getLogger().println("Error checking Sauce Connect run condition");
                        throw new IOException(e);
                    }
                    if (shouldClose) {
                        listener.getLogger().println("Shutting down Sauce Connect");
                        String resolvedOptions = getCommandLineOptions(build, listener);
                        if (launchSauceConnectOnSlave) {
                            Computer.currentComputer().getChannel().call(new SauceConnectCloser(listener, getUserName(), resolvedOptions, useOldSauceConnect));
                        } else {
                            SauceConnectCloser tunnelCloser = new SauceConnectCloser(listener, getUserName(), resolvedOptions, useOldSauceConnect);
                            tunnelCloser.call();
                        }
                    }
                }

                processBuildOutput(build);
                logParserMap.remove(build.toString());
                listener.getLogger().println("Finished post-build for Sauce Labs plugin");
                return true;
            }
        };
    }

    /**
     * Returns the command line options to be used as part of Sauce Connect.  Any variable references contained in the
     * options specified within the Jenkins job configuration are resolved, and if common options are specified then these are appended to the list of options.
     *
     * @param build    The build in progress for which an {@link Environment} object is created.
     *                 Never null.
     * @param listener Can be used to send any message.
     * @return String representing the Sauce Connect command line options
     * @throws IOException
     * @throws InterruptedException
     */
    private String getCommandLineOptions(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {

        StringBuilder resolvedOptions = new StringBuilder();
        resolvedOptions.append(getResolvedOptions(build, listener, options));
        String resolvedCommonOptions = getResolvedOptions(build, listener, PluginImpl.get().getSauceConnectOptions());
        if (resolvedCommonOptions != null && !resolvedCommonOptions.equals("")) {
            if (!resolvedOptions.toString().equals("")) {
                resolvedOptions.append(' ');
            }
            resolvedOptions.append(resolvedCommonOptions);
        }
        return resolvedOptions.toString();
    }

    /**
     * Returns the Sauce Connect options, with any strings representing environment variables (eg. ${SOME_ENV_VAR}) resolved.
     *
     * @param build    The same {@link Build} object given to the set up method.
     * @param listener The same {@link BuildListener} object given to the set up method.
     * @param options  The command line options to resolve
     * @return the Sauce Connect options to be used for the build
     * @throws IOException
     * @throws InterruptedException
     */
    private String getResolvedOptions(AbstractBuild build, BuildListener listener, String options) throws IOException, InterruptedException {
        if (options == null) {
            return "";
        }
        VariableResolver.ByMap<String> variableResolver = new VariableResolver.ByMap<String>(build.getEnvironment(listener));
        return Util.replaceMacro(options, variableResolver);
    }

    /**
     * Adds a new {@link SauceOnDemandBuildAction} instance to the {@link AbstractBuild} instance. The
     * processing of the build output will be performed by the {@link SauceOnDemandReportPublisher} instance (which
     * is created if the 'Embed Sauce OnDemand Reports' option is selected.
     *
     * @param build the build in progress
     */
    private void processBuildOutput(AbstractBuild build) {
        if (logParserMap != null) {
            logger.fine("Adding build action to " + build.toString());
            SauceOnDemandLogParser logParser = logParserMap.get(build.toString());
            if (logParser != null) {
                SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, logParser, getUserName(), getApiKey());
                build.addAction(buildAction);
            }
        }
    }

    /**
     * Replace all spaces and hashes with underscores.
     *
     * @param buildNumber the current Jenkins build number
     * @return the build number with all non-alphanumeric characters replaced with _
     */
    public static String sanitiseBuildNumber(String buildNumber) {
        return buildNumber.replaceAll("[^A-Za-z0-9]", "_");
    }

    /**
     * @return the hostname for the current environment.
     */
    private String getCurrentHostName() {
        try {
            String hostName = Computer.currentComputer().getHostName();
            if (hostName != null) {
                return hostName;
            }
        } catch (UnknownHostException e) {
            //shouldn't happen
            logger.log(Level.SEVERE, "Unable to retrieve host name", e);
        } catch (InterruptedException e) {
            //shouldn't happen
            logger.log(Level.SEVERE, "Unable to retrieve host name", e);
        } catch (IOException e) {
            //shouldn't happen
            logger.log(Level.SEVERE, "Unable to retrieve host name", e);
        }
        return "localhost";
    }


    /**
     * @return the port to be used
     */
    private int getPort() {
        if (StringUtils.isNotBlank(seleniumPort) && !seleniumPort.equals("0")) {
            Matcher matcher = ENVIRONMENT_VARIABLE_PATTERN.matcher(seleniumPort);
            if (matcher.matches()) {
                String variableName = matcher.group(1);
                String value = System.getenv(variableName);
                if (value == null) {
                    value = "0";
                }
                return Integer.parseInt(value);
            } else {
                return Integer.parseInt(seleniumPort);
            }
        } else {
            if (isEnableSauceConnect()) {
                return 4445;
            } else {
                return 4444;
            }
        }
    }

    /**
     * @param build    The build in progress for which an {@link Environment} object is created.
     *                 Never null.
     * @param listener Can be used to send any message.
     * @return File instance representing the Sauce Connect jar file
     * @throws IOException
     * @deprecated will be removed when Sauce Connect 3 support is dropped
     */
    private File copySauceConnectToSlave(AbstractBuild build, BuildListener listener) throws IOException {

        if (isUseOldSauceConnect()) {
            FilePath projectWorkspaceOnSlave = build.getProject().getSomeWorkspace();
            try {
                File sauceConnectJar = SauceConnectUtils.extractSauceConnectJarFile();
                MyFilePath.copyRecursiveTo(
                        new FilePath(sauceConnectJar.getParentFile()),
                        sauceConnectJar.getName(),
                        null,
                        false, false, projectWorkspaceOnSlave);

                return new File(projectWorkspaceOnSlave.getRemote(), sauceConnectJar.getName());
            } catch (URISyntaxException e) {
                listener.error("Error copying sauce connect jar to slave", e);
            } catch (InterruptedException e) {
                listener.error("Error copying sauce connect jar to slave", e);
            }
        }
        return null;
    }

    /**
     * @return the Sauce username to be used
     */
    public String getUserName() {
        if (getCredentials() != null) {
            return getCredentials().getUsername();
        } else {
            PluginImpl p = PluginImpl.get();
            if (p.isReuseSauceAuth()) {
                SauceOnDemandAuthentication storedCredentials = null;
                storedCredentials = new SauceOnDemandAuthentication();
                return storedCredentials.getUsername();
            } else {
                return p.getUsername();

            }
        }
    }

    /**
     * @return the Sauce access key to be used
     */
    public String getApiKey() {
        if (getCredentials() != null) {
            return getCredentials().getApiKey();
        } else {
            PluginImpl p = PluginImpl.get();
            if (p.isReuseSauceAuth()) {
                SauceOnDemandAuthentication storedCredentials;
                storedCredentials = new SauceOnDemandAuthentication();
                return storedCredentials.getAccessKey();
            } else {
                return Secret.toString(p.getApiKey());
            }
        }
    }

    public boolean isUseLatestVersion() {
        return useLatestVersion;
    }

    public String getSeleniumHost() {
        return seleniumHost;
    }

    public void setSeleniumHost(String seleniumHost) {
        this.seleniumHost = seleniumHost;
    }

    public String getSeleniumPort() {
        return seleniumPort;
    }

    public void setSeleniumPort(String seleniumPort) {
        this.seleniumPort = seleniumPort;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public SeleniumInformation getSeleniumInformation() {
        return seleniumInformation;
    }

    public void setSeleniumInformation(SeleniumInformation seleniumInformation) {
        this.seleniumInformation = seleniumInformation;
    }

    public boolean isEnableSauceConnect() {
        return enableSauceConnect;
    }

    public void setEnableSauceConnect(boolean enableSauceConnect) {
        this.enableSauceConnect = enableSauceConnect;
    }

    public List<String> getWebDriverBrowsers() {
        return webDriverBrowsers;
    }

    public void setWebDriverBrowsers(List<String> webDriverBrowsers) {
        this.webDriverBrowsers = webDriverBrowsers;
    }

    public List<String> getAppiumBrowsers() {
        return appiumBrowsers;
    }

    public void setAppiumBrowsers(List<String> appiumBrowsers) {
        this.appiumBrowsers = appiumBrowsers;
    }

    public boolean isLaunchSauceConnectOnSlave() {
        return launchSauceConnectOnSlave;
    }

    public void setLaunchSauceConnectOnSlave(boolean launchSauceConnectOnSlave) {
        this.launchSauceConnectOnSlave = launchSauceConnectOnSlave;
    }

    public boolean isUseOldSauceConnect() {
        return useOldSauceConnect;
    }

    public void setUseOldSauceConnect(boolean useOldSauceConnect) {
        this.useOldSauceConnect = useOldSauceConnect;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    public String getHttpsProtocol() {
        return httpsProtocol;
    }

    public void setHttpsProtocol(String httpsProtocol) {
        this.httpsProtocol = httpsProtocol;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getStartingURL() {
        return startingURL;
    }

    public RunCondition getCondition() {
        return condition;
    }

    public String getSauceConnectPath() {
        return sauceConnectPath;
    }

    public void setSauceConnectPath(String sauceConnectPath) {
        this.sauceConnectPath = sauceConnectPath;
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new {@link SauceOnDemandLogParser} instance, which is added to the {@link #logParserMap}.
     */
    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
        SauceOnDemandLogParser sauceOnDemandLogParser = new SauceOnDemandLogParser(logger, build.getCharset());
        if (logParserMap == null) {
            logParserMap = new ConcurrentHashMap<String, SauceOnDemandLogParser>();
        }
        logParserMap.put(build.toString(), sauceOnDemandLogParser);
        return sauceOnDemandLogParser;
    }

    /**
     * Handles terminating any running Sauce Connect processes.
     */
    private static final class SauceConnectCloser implements Callable<SauceConnectCloser, AbstractSauceTunnelManager.SauceConnectException> {

        private final BuildListener listener;
        private final String username;
        private final String options;
        private final boolean useOldSauceConnect;

        public SauceConnectCloser(final BuildListener listener, final String username, String options, boolean useOldSauceConnect) {
            this.listener = listener;
            this.username = username;
            this.options = options;
            this.useOldSauceConnect = useOldSauceConnect;
        }

        /**
         * {@inheritDoc}
         *
         * Closes the Sauce Connect tunnel.
         */
        public SauceConnectCloser call() throws AbstractSauceTunnelManager.SauceConnectException {
            try {
                getSauceTunnelManager(useOldSauceConnect).closeTunnelsForPlan(username, options, listener.getLogger());
            } catch (ComponentLookupException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            }
            return this;
        }
    }

    /**
     * Handles starting Sauce Connect.
     */
    private static final class SauceConnectHandler implements Callable<SauceConnectHandler, AbstractSauceTunnelManager.SauceConnectException> {
        private final String options;
        private final String workingDirectory;
        private final String username;
        private final String key;

        private final BuildListener listener;
        private final boolean useOldSauceConnect;
        private final String httpsProtocol;
        private final boolean verboseLogging;
        private final String sauceConnectPath;
        private File sauceConnectJar;
        private int port;

        /**
         * @param sauceOnDemandBuildWrapper
         * @param listener
         * @param workingDirectory
         * @param resolvedOptions
         */
        public SauceConnectHandler(SauceOnDemandBuildWrapper sauceOnDemandBuildWrapper, BuildListener listener, String workingDirectory, String resolvedOptions) {
            this.options = resolvedOptions;
            this.workingDirectory = workingDirectory;
            this.listener = listener;
            this.username = sauceOnDemandBuildWrapper.getUserName();
            this.port = sauceOnDemandBuildWrapper.getPort();
            this.key = sauceOnDemandBuildWrapper.getApiKey();
            this.useOldSauceConnect = sauceOnDemandBuildWrapper.isUseOldSauceConnect();
            this.httpsProtocol = sauceOnDemandBuildWrapper.getHttpsProtocol();
            this.verboseLogging = sauceOnDemandBuildWrapper.isVerboseLogging();
            this.sauceConnectPath = sauceOnDemandBuildWrapper.getSauceConnectPath();
        }

        /**
         * @param sauceOnDemandBuildWrapper
         * @param listener
         * @param workingDirectory
         * @param resolvedOptions
         * @param sauceConnectJar
         */
        public SauceConnectHandler(SauceOnDemandBuildWrapper sauceOnDemandBuildWrapper, BuildListener listener, String workingDirectory, String resolvedOptions, File sauceConnectJar) {
            this(sauceOnDemandBuildWrapper, listener, workingDirectory, resolvedOptions);
            this.sauceConnectJar = sauceConnectJar;
        }

        /**
         * Launches Sauce Connect.
         *
         * @return the current SauceConnectHandler instance
         * @throws AbstractSauceTunnelManager.SauceConnectException
         *          thrown if an error occurs starting Sauce Connect
         */
        public SauceConnectHandler call() throws AbstractSauceTunnelManager.SauceConnectException {

            try {
                listener.getLogger().println("Launching Sauce Connect on " + InetAddress.getLocalHost().getHostName());
                if (useOldSauceConnect) {
                    listener.getLogger().println("*** Support for Sauce Connect v3 is scheduled to end on 19 August 2015 *** ");
                    listener.getLogger().println("*** Please update your settings to use Sauce Connect v4 *** ");
                }

                AbstractSauceTunnelManager sauceTunnelManager = getSauceTunnelManager(useOldSauceConnect);
                if (sauceTunnelManager instanceof HudsonSauceConnectFourManager && workingDirectory != null) {
                    ((HudsonSauceConnectFourManager) sauceTunnelManager).setWorkingDirectory(workingDirectory);
                }
                sauceTunnelManager.setSauceRest(new JenkinsSauceREST(username, key));

                Process process = sauceTunnelManager.openConnection(username, key, port, sauceConnectJar, options, httpsProtocol, listener.getLogger(), verboseLogging, sauceConnectPath);
                return this;
            } catch (ComponentLookupException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            } catch (UnknownHostException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            }
        }
    }

    /**
     * Retrieve the {@link AbstractSauceTunnelManager} instance to be used to launch Sauce Connect.
     *
     * @param useOldSauceConnect indicates whether Sauce Connect v3 should be launched
     * @return {@link AbstractSauceTunnelManager} instance
     * @throws ComponentLookupException
     */
    public static AbstractSauceTunnelManager getSauceTunnelManager(boolean useOldSauceConnect) throws ComponentLookupException {
        return useOldSauceConnect ? HudsonSauceManagerFactory.getInstance().createSauceConnectTwoManager() :
                HudsonSauceManagerFactory.getInstance().createSauceConnectFourManager();
    }

    /**
     * Plugin descriptor, which adds the plugin details to the Jenkins job configuration page.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        /** Handles retrieving details for supported browsers. */
        private static final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

        /**
         *
         * @return text to be displayed within Jenkins job configuration
         */
        @Override
        public String getDisplayName() {
            return "Sauce Labs Support";
        }


        /**
         * @return the list of supported Appium browsers
         */
        public List<Browser> getAppiumBrowsers() {
            try {
                return BROWSER_FACTORY.getAppiumBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }

        /**
         * @return the list of supported Selenium RC browsers
         */
        public List<Browser> getSeleniumBrowsers() {
            try {
                return BROWSER_FACTORY.getSeleniumBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }

        /**
         * @return the list of supported WebDriver browsers
         */
        public List<Browser> getWebDriverBrowsers() {
            try {
                return BROWSER_FACTORY.getWebDriverBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }

        /**
         * @return the list of supported WebDriver browsers
         */
        public Map<String,List<Browser>> getWebDriverMap() {
            try {
                Map<String,List<Browser>> map = new HashMap<String,List<Browser>>();
                for (Browser browser : BROWSER_FACTORY.getWebDriverBrowsers()) {
                    List<Browser> browsers = map.get(browser.getOs());
                    if (browsers == null) {
                        browsers = new ArrayList<Browser>();
                        map.put(browser.getOs(), browsers);
                    }
                    browsers.add(browser);
                }
                return map;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyMap();
        }
    }


    /**
     * Captures the output of a Jenkins build, so that it can be parsed to find Sauce jobs invoked by the build.
     *
     * @author Ross Rowe
     */
    public class SauceOnDemandLogParser extends LineTransformationOutputStream implements Serializable {

        private transient OutputStream outputStream;
        private transient Charset charset;
        private List<String> lines;

        public SauceOnDemandLogParser(OutputStream outputStream, Charset charset) {
            this.outputStream = outputStream;
            this.charset = charset;
            this.lines = new ArrayList<String>();
        }

        /**
         * {@inheritDoc}
         *
         * Decodes the line and add it to the {@link #lines} list.
         */
        @Override
        protected void eol(byte[] b, int len) throws IOException {
            if (this.outputStream != null) {
                this.outputStream.write(b, 0, len);
            }
            if (charset != null) {
                lines.add(charset.decode(ByteBuffer.wrap(b, 0, len)).toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            super.close();
            if (outputStream != null) {
                this.outputStream.close();
            }
        }

        public List<String> getLines() {
            return lines;
        }
    }


}
