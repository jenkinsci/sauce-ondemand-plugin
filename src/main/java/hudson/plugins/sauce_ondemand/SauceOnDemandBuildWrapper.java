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
import hudson.console.LineTransformationOutputStream;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.tasks.BuildWrapper;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    public static final String SELENIUM_DRIVER = "SELENIUM_DRIVER";
    public static final String SAUCE_ONDEMAND_BROWSERS = "SAUCE_ONDEMAND_BROWSERS";
    public static final String SELENIUM_HOST = "SELENIUM_HOST";
    public static final String SELENIUM_PORT = "SELENIUM_PORT";
    public static final String SELENIUM_STARTING_URL = "SELENIUM_STARTING_URL";
    private static final String SAUCE_USERNAME = "SAUCE_USER_NAME";
    private static final String SAUCE_API_KEY = "SAUCE_API_KEY";
    public static final String SELENIUM_DEVICE = "SELENIUM_DEVICE";
    public static final String SELENIUM_DEVICE_TYPE = "SELENIUM_DEVICE_TYPE";
    public static final Pattern ENVIRONMENT_VARIABLE_PATTERN = Pattern.compile("[$|%]([a-zA-Z_][a-zA-Z0-9_]+)");
    public static final String SELENIUM_BROWSER = "SELENIUM_BROWSER";
    public static final String SELENIUM_PLATFORM = "SELENIUM_PLATFORM";
    public static final String SELENIUM_VERSION = "SELENIUM_VERSION";
    private static final String JENKINS_BUILD_NUMBER = "JENKINS_BUILD_NUMBER";

    private static final long serialVersionUID = 1L;
    private final String startingURL;
    private boolean useOldSauceConnect;
    private boolean enableSauceConnect;
    private Map<String, SauceOnDemandLogParser> logParserMap;
    private String seleniumHost;
    private String seleniumPort;
    private Credentials credentials;
    private SeleniumInformation seleniumInformation;
    private List<String> seleniumBrowsers;
    private List<String> webDriverBrowsers;
    private List<String> appiumBrowsers;
    /**
     * Default behaviour is to launch Sauce Connect on the slave node.
     */
    private boolean launchSauceConnectOnSlave = true;

    private String httpsProtocol;
    private String options;
    /**
     * Default verbose logging to true.
     */
    private boolean verboseLogging = true;
    private transient SauceConnectHandler sauceConnectHandler;

    /**
     * Constructs a new instance using data entered on the job configuration screen.
     *
     * @param credentials
     * @param seleniumInformation
     * @param seleniumHost
     * @param seleniumPort
     * @param httpsProtocol
     * @param options
     * @param startingURL
     * @param enableSauceConnect
     * @param launchSauceConnectOnSlave
     * @param useOldSauceConnect
     * @param verboseLogging
     */
    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(Credentials credentials,
                                     SeleniumInformation seleniumInformation,
                                     String seleniumHost,
                                     String seleniumPort,
                                     String httpsProtocol,
                                     String options,
                                     String startingURL,
                                     boolean enableSauceConnect,
                                     boolean launchSauceConnectOnSlave,
                                     boolean useOldSauceConnect,
                                     boolean verboseLogging) {
        this.credentials = credentials;
        this.seleniumInformation = seleniumInformation;
        this.enableSauceConnect = enableSauceConnect;
        this.seleniumHost = seleniumHost;
        this.seleniumPort = seleniumPort;
        this.httpsProtocol = httpsProtocol;
        this.options = options;
        this.startingURL = startingURL;
        if (seleniumInformation != null) {
            this.seleniumBrowsers = seleniumInformation.getSeleniumBrowsers();
            this.webDriverBrowsers = seleniumInformation.getWebDriverBrowsers();
            this.appiumBrowsers = seleniumInformation.getAppiumBrowsers();
        }
        this.launchSauceConnectOnSlave = launchSauceConnectOnSlave;
        this.useOldSauceConnect = useOldSauceConnect;
        this.verboseLogging = verboseLogging;
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
        logger.fine("Setting up Sauce Build Wrapper");
        if (isEnableSauceConnect()) {
            String workingDirectory = PluginImpl.get().getSauceConnectDirectory();
            String resolvedOptions = getResolvedOptions(build, listener);
            if (launchSauceConnectOnSlave) {
                listener.getLogger().println("Starting Sauce Connect on slave node using tunnel identifier: " + AbstractSauceTunnelManager.getTunnelIdentifier(resolvedOptions, "default"));

                if (useOldSauceConnect && !(Computer.currentComputer() instanceof Hudson.MasterComputer)) {
                    //only copy sauce connect jar if we are using Sauce Connect v3
                    File sauceConnectJar = copySauceConnectToSlave(build, listener);
                    sauceConnectHandler = Computer.currentComputer().getChannel().call(
                            new SauceConnectHandler(
                                    listener,
                                    getPort(),
                                    workingDirectory,
                                    sauceConnectJar,
                                    resolvedOptions));
                } else {
                    sauceConnectHandler = Computer.currentComputer().getChannel().call
                            (new SauceConnectHandler(
                                    listener,
                                    getPort(),
                                    workingDirectory,
                                    resolvedOptions));
                }
            } else {
                listener.getLogger().println("Starting Sauce Connect on master node using identifier: " + AbstractSauceTunnelManager.getTunnelIdentifier(resolvedOptions, "default"));
                //launch Sauce Connect on the master
                SauceConnectHandler sauceConnectStarter = new SauceConnectHandler(listener, getPort(), workingDirectory, resolvedOptions);
                sauceConnectHandler = sauceConnectStarter.call();
            }
        }

        return new Environment() {

            /**
             * Updates the environment variable map to include the Sauce specific environment variables applicable to the build.
             * @param env existing environment variables
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                logger.fine("Creating Sauce environment variables");
                SauceEnvironmentUtil.outputSeleniumRCVariables(env, seleniumBrowsers, getUserName(), getApiKey());
                SauceEnvironmentUtil.outputWebDriverVariables(env, webDriverBrowsers, getUserName(), getApiKey());
                SauceEnvironmentUtil.outputAppiumVariables(env, appiumBrowsers, getUserName(), getApiKey());
                //if any variables have been defined in build variables (ie. by a multi-config project), use them
                Map buildVariables = build.getBuildVariables();
                String buildName = build.getProject().getName();
                if (buildVariables.containsKey(SELENIUM_BROWSER)) {
                    env.put(SELENIUM_BROWSER, (String) buildVariables.get(SELENIUM_BROWSER));
                }
                if (buildVariables.containsKey(SELENIUM_VERSION)) {
                    env.put(SELENIUM_VERSION, (String) buildVariables.get(SELENIUM_VERSION));
                }
                if (buildVariables.containsKey(SELENIUM_PLATFORM)) {
                    env.put(SELENIUM_PLATFORM, (String) buildVariables.get(SELENIUM_PLATFORM));
                }
                env.put(JENKINS_BUILD_NUMBER, sanitiseBuildNumber(build.toString()));
                env.put(SAUCE_USERNAME, getUserName());
                env.put(SAUCE_API_KEY, getApiKey());
                env.put(SELENIUM_HOST, getHostName());

                DecimalFormat myFormatter = new DecimalFormat("####");
                env.put(SELENIUM_PORT, myFormatter.format(getPort()));
                if (getStartingURL() != null) {
                    env.put(SELENIUM_STARTING_URL, getStartingURL());
                }
            }

            /**
             *
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
             *
             */
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {

                if (sauceConnectHandler != null) {
                    listener.getLogger().println("Shutting down Sauce Connect");
                    if (launchSauceConnectOnSlave) {
                        Computer.currentComputer().getChannel().call(new SauceConnectCloser(listener, getUserName()));
                    } else {
                        SauceConnectCloser tunnelCloser = new SauceConnectCloser(listener, getUserName());
                        tunnelCloser.call();
                    }
                }

                processBuildOutput(build);
                logParserMap.remove(build.toString());
                return true;
            }
        };
    }

    /**
     * Returns the Sauce Connect options, with any strings representing environment variables (eg. ${SOME_ENV_VAR}) resolved.
     *
     * @param build    The same {@link Build} object given to the set up method.
     * @param listener The same {@link BuildListener} object given to the set up method.
     * @return the Sauce Connect options to be used for the build
     * @throws IOException
     * @throws InterruptedException
     */
    private String getResolvedOptions(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        String resolvedOptions = options;
        if (options != null) {
            //check to see if options contains any environment variables to be resolved
            Pattern pattern = Pattern.compile("(\\$\\{.+\\})");
            Matcher matcher = pattern.matcher(options);
            while (matcher.find()) {
                String match = matcher.group();
                String key = match.replaceAll("[\\$\\{\\}]", "");
                if (build.getEnvironment(listener).containsKey(key)) {
                    resolvedOptions = resolvedOptions.replace(match, build.getEnvironment().get(key));
                }
            }

        }
        return resolvedOptions;
    }

    /**
     * Adds a new {@link SauceOnDemandBuildAction} instance to the {@link AbstractBuild} instance. The
     * processing of the build output will be performed by the {@link SauceOnDemandReportPublisher} instance (which
     * is created if the 'Embed Sauce OnDemand Reports' option is selected.
     *
     * @param build the build in progress
     */
    private void processBuildOutput(AbstractBuild build) {
        logger.fine("Adding build action to " + build.toString());
        SauceOnDemandLogParser logParser = logParserMap.get(build.toString());
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, logParser, getUserName(), getApiKey());
        build.addAction(buildAction);
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
     * @param build
     * @param listener
     * @return
     * @throws IOException
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
     *
     */
    private final class TunnelHolder implements Serializable {

        public void close(TaskListener listener) {
            try {
                getSauceTunnelManager().closeTunnelsForPlan(getUserName(), options, listener.getLogger());
            } catch (ComponentLookupException e) {
                //shouldn't happen
                logger.log(Level.SEVERE, "Unable to close tunnel", e);
            }

        }
    }

    /**
     * Handles terminating any running Sauce Connect processes.
     */
    private final class SauceConnectCloser implements Callable<SauceConnectCloser, AbstractSauceTunnelManager.SauceConnectException> {

        private final BuildListener listener;

        private final String username;

        public SauceConnectCloser(final BuildListener listener, final String username) {
            this.listener = listener;
            this.username = username;
        }

        public SauceConnectCloser call() throws AbstractSauceTunnelManager.SauceConnectException {
            try {
                getSauceTunnelManager().closeTunnelsForPlan(username, options, listener.getLogger());
            } catch (ComponentLookupException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            }
            return this;
        }
    }

    /**
     * Handles starting Sauce Connect.
     */
    private final class SauceConnectHandler implements Callable<SauceConnectHandler, AbstractSauceTunnelManager.SauceConnectException> {
        private final String options;
        private final String workingDirectory;
        private String username;
        private String key;

        private BuildListener listener;
        private File sauceConnectJar;
        private int port;

        /**
         * Constructs a new instance.
         * @param listener
         * @param port
         * @param options
         * @throws IOException
         */
        public SauceConnectHandler(BuildListener listener, int port, String workingDirectory, String options) throws IOException {
            this.username = getUserName();
            this.key = getApiKey();
            this.listener = listener;
            this.port = port;
            this.workingDirectory = workingDirectory;
            this.options = options;

        }

        /**
         * Constructs a new instance.
         *
         * @param listener
         * @param port
         * @param sauceConnectJar
         * @param options
         * @throws IOException
         */
        public SauceConnectHandler(BuildListener listener, int port, String workingDirectory,File sauceConnectJar, String options) throws IOException {
            this(listener, port, workingDirectory, options);
            this.sauceConnectJar = sauceConnectJar;

        }

        /**
         * Launches Sauce Connect.
         *
         * @return the current SauceConnectHandler instance
         * @throws AbstractSauceTunnelManager.SauceConnectException thrown if an error occurs starting Sauce Connect
         */
        public SauceConnectHandler call() throws AbstractSauceTunnelManager.SauceConnectException {

            try {
                listener.getLogger().println("Launching Sauce Connect on " + InetAddress.getLocalHost().getHostName());
                AbstractSauceTunnelManager sauceTunnelManager = getSauceTunnelManager();
                if (sauceTunnelManager instanceof HudsonSauceConnectFourManager && workingDirectory != null) {
                    ((HudsonSauceConnectFourManager) sauceTunnelManager).setWorkingDirectory(workingDirectory);
                }
                Process process = sauceTunnelManager.openConnection(username, key, port, sauceConnectJar, options, httpsProtocol, listener.getLogger(), verboseLogging);
                return this;
            } catch (ComponentLookupException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            } catch (UnknownHostException e) {
                throw new AbstractSauceTunnelManager.SauceConnectException(e);
            }
        }
    }

    public AbstractSauceTunnelManager getSauceTunnelManager() throws ComponentLookupException {
        return useOldSauceConnect ? HudsonSauceManagerFactory.getInstance().createSauceConnectTwoManager() :
                HudsonSauceManagerFactory.getInstance().createSauceConnectFourManager();
    }

    /**
     * Plugin descriptor, which adds the plugin details to the Jenkins job configuration page.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        public static final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

        @Override
        public BuildWrapper newInstance(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Support";
        }


        /**
         * The list of supported Appium devices isn't available via the Sauce REST API, so we hard-code
         * the combinations.
         *
         * @return
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
         *
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
         *
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
    }


    /**
     * Captures the output of a Jenkins build, so that it can be parsed to find Sauce jobs invoked by the build.
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

        @Override
        protected void eol(byte[] b, int len) throws IOException {
            if (this.outputStream != null) {
                this.outputStream.write(b, 0, len);
            }
            if (charset != null) {
                lines.add(charset.decode(ByteBuffer.wrap(b, 0, len)).toString());
            }
        }

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
