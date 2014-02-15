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
import com.saucelabs.ci.sauceconnect.SauceConnectUtils;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.common.SauceOnDemandAuthentication;
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
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandBuildWrapper extends BuildWrapper implements Serializable {

    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildWrapper.class.getName());
    public static final String SELENIUM_DRIVER = "SELENIUM_DRIVER";
    public static final String SAUCE_ONDEMAND_BROWSERS = "SAUCE_ONDEMAND_BROWSERS";
    public static final String SELENIUM_HOST = "SELENIUM_HOST";
    public static final String SELENIUM_PORT = "SELENIUM_PORT";
    public static final String SELENIUM_STARTING_URL = "SELENIUM_STARTING_URL";
    private static final String SAUCE_USERNAME = "SAUCE_USER_NAME";
    private static final String SAUCE_API_KEY = "SAUCE_API_KEY";
    private final String startingURL;

    private boolean enableSauceConnect;

    private static final long serialVersionUID = 1L;

    private ITunnelHolder tunnels;
    private String seleniumHost;
    private String seleniumPort;
    private Credentials credentials;
    private SeleniumInformation seleniumInformation;
    private List<String> seleniumBrowsers;
    private List<String> webDriverBrowsers;
    /**
     *
     */
    private boolean launchSauceConnectOnSlave = false;
    public static final Pattern ENVIRONMENT_VARIABLE_PATTERN = Pattern.compile("[$|%]([a-zA-Z_][a-zA-Z0-9_]+)");
    public static final String SELENIUM_BROWSER = "SELENIUM_BROWSER";
    public static final String SELENIUM_PLATFORM = "SELENIUM_PLATFORM";
    public static final String SELENIUM_VERSION = "SELENIUM_VERSION";
    private Map<String, SauceOnDemandLogParser> logParserMap;
    private static final String JENKINS_BUILD_NUMBER = "JENKINS_BUILD_NUMBER";
    private String httpsProtocol;
    private String options;


    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(Credentials
                                             credentials,
                                     SeleniumInformation seleniumInformation,
                                     String seleniumHost,
                                     String seleniumPort,
                                     String httpsProtocol,
                                     String options,
                                     String startingURL,
                                     boolean enableSauceConnect,
                                     boolean launchSauceConnectOnSlave) {
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
        }
        this.launchSauceConnectOnSlave = launchSauceConnectOnSlave;
    }


    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        logger.info("Setting up Sauce Build Wrapper v1.64");
        if (isEnableSauceConnect()) {
            if (launchSauceConnectOnSlave) {
                listener.getLogger().println("Starting Sauce OnDemand SSH tunnel on slave node");
                if (!(Computer.currentComputer() instanceof Hudson.MasterComputer)) {
                    File sauceConnectJar = copySauceConnectToSlave(build, listener);
                    tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(listener, getPort(), sauceConnectJar));
                } else {
                    tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(listener, getPort()));
                }
            } else {
                listener.getLogger().println("Starting Sauce OnDemand SSH tunnel on master node");
                //launch Sauce Connect on the master
                SauceConnectStarter sauceConnectStarter = new SauceConnectStarter(listener, getPort());
                tunnels = sauceConnectStarter.call();
            }
        }

        return new Environment() {

            /**
             *
             * @param env
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                logger.info("Creating Sauce environment variables");
                SauceEnvironmentUtil.outputWebDriverVariables(env, seleniumBrowsers, getUserName(), getApiKey());
                SauceEnvironmentUtil.outputWebDriverVariables(env, webDriverBrowsers, getUserName(), getApiKey());
                //if any variables have been defined in build variables (ie. by a multi-config project), use them
                Map buildVariables = build.getBuildVariables();
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
             * @return
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


            private String getStartingURL() {
                return startingURL;
            }

            /**
             *
             * @param build
             * @param listener
             * @return
             * @throws IOException
             * @throws InterruptedException
             */
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (tunnels != null) {
                    listener.getLogger().println("Shutting down Sauce Connect SSH tunnels");
                    if (launchSauceConnectOnSlave) {
                        Computer.currentComputer().getChannel().call(new SauceConnectCloser(tunnels, listener));
                    } else {
                        SauceConnectCloser tunnelCloser = new SauceConnectCloser(tunnels, listener);
                        tunnelCloser.call();
                    }
                    listener.getLogger().println("Sauce Connect closed");
                }
                processBuildOutput(build);
                logParserMap.remove(build.toString());
                return true;
            }
        };
    }

    /**
     * Adds a new {@link SauceOnDemandBuildAction} instance to the <code>build</code> instance. The
     * processing of the build output will be performed by the {@link SauceOnDemandReportPublisher} instance (which
     * is created if the 'Embed Sauce OnDemand Reports' option is selected.
     *
     * @param build
     */
    private void processBuildOutput(AbstractBuild build) {
        logger.info("Adding build action to " + build.toString());
        SauceOnDemandLogParser logParser = logParserMap.get(build.toString());
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, logParser, getUserName(), getApiKey());
        build.addAction(buildAction);
    }

    /**
     * Replace all spaces and hashes with underscores.
     *
     * @param buildNumber
     * @return
     */
    public static String sanitiseBuildNumber(String buildNumber) {
        return buildNumber.replaceAll("[^A-Za-z0-9]", "_");
    }

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

    private File copySauceConnectToSlave(AbstractBuild build, BuildListener listener) throws IOException {

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

    private interface ITunnelHolder {
        void close(TaskListener listener);
    }

    public boolean isLaunchSauceConnectOnSlave() {
        return launchSauceConnectOnSlave;
    }

    public void setLaunchSauceConnectOnSlave(boolean launchSauceConnectOnSlave) {
        this.launchSauceConnectOnSlave = launchSauceConnectOnSlave;
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

    private static final class TunnelHolder implements ITunnelHolder, Serializable {
        private String username;

        public TunnelHolder(String username) {
            this.username = username;
        }

        public void close(TaskListener listener) {
            try {
                HudsonSauceManagerFactory.getInstance().createSauceConnectManager().closeTunnelsForPlan(username, listener.getLogger());
            } catch (ComponentLookupException e) {
                //shouldn't happen
                logger.log(Level.SEVERE, "Unable to close tunnel", e);
            }

        }
    }

    private final class SauceConnectCloser implements Callable<ITunnelHolder, IOException> {

        private ITunnelHolder tunnelHolder;
        private BuildListener listener;


        public SauceConnectCloser(ITunnelHolder tunnelHolder, BuildListener listener) {
            this.tunnelHolder = tunnelHolder;
            this.listener = listener;
        }

        public ITunnelHolder call() throws IOException {
            tunnelHolder.close(listener);
            return tunnelHolder;
        }
    }


    private final class SauceConnectStarter implements Callable<ITunnelHolder, IOException> {
        private String username;
        private String key;

        private BuildListener listener;
        private File sauceConnectJar;
        private int port;

        public SauceConnectStarter(BuildListener listener, int port) throws IOException {
            this.username = getUserName();
            this.key = getApiKey();
            this.listener = listener;
            this.port = port;
        }

        public SauceConnectStarter(BuildListener listener, int port, File sauceConnectJar) throws IOException {
            this(listener, port);
            this.sauceConnectJar = sauceConnectJar;

        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder tunnelHolder = new TunnelHolder(username);
            SauceTunnelManager sauceManager = null;
            try {
                listener.getLogger().println("Launching Sauce Connect on " + InetAddress.getLocalHost().getHostName());
                sauceManager = HudsonSauceManagerFactory.getInstance().createSauceConnectManager();
                Process process = sauceManager.openConnection(username, key, port, sauceConnectJar, options, httpsProtocol, listener.getLogger());
                return tunnelHolder;
            } catch (ComponentLookupException e) {
                throw new IOException(e);
            }

        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        @Override
        public BuildWrapper newInstance(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Support";
        }


        public List<Browser> getSeleniumBrowsers() {
            try {
                return BrowserFactory.getInstance().getSeleniumBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }

        public List<Browser> getWebDriverBrowsers() {
            try {
                return BrowserFactory.getInstance().getWebDriverBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }
    }


    /**
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
