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
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceConnectUtils;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandBuildWrapper extends BuildWrapper implements Serializable {

    private boolean enableSauceConnect;

    private static final long serialVersionUID = 1L;

    private ITunnelHolder tunnels;
    private String seleniumHost;
    private int seleniumPort;
    private Credentials credentials;
    private SeleniumInformation seleniumInformation;

    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(Credentials
                                             credentials, SeleniumInformation seleniumInformation, String seleniumHost, int seleniumPort, boolean enableSauceConnect) {
        this.credentials = credentials;
        this.seleniumInformation = seleniumInformation;
        this.enableSauceConnect = enableSauceConnect;
        this.seleniumHost = seleniumHost;
        this.seleniumPort = seleniumPort;
    }


    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting Sauce OnDemand SSH tunnels");
        String buildNameDigest = Util.getDigestOf(build.getFullDisplayName());
        if (isEnableSauceConnect()) {
            if (!(Computer.currentComputer() instanceof Hudson.MasterComputer)) {
                File sauceConnectJar = copySauceConnectToSlave(build, listener);
                tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(buildNameDigest, listener, getPort(), sauceConnectJar));
            } else {
                tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(buildNameDigest, listener, getPort()));
            }
        }

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("SAUCE_ONDEMAND_HOST", getHostName());
                env.put("SAUCE_ONDEMAND_PORT", Integer.toString(getPort()));
                if (getStartingURL() != null) {
                    env.put("SELENIUM_STARTING_URL", getStartingURL());
                }
            }

            private String getHostName() {
                if (StringUtils.isNotBlank(seleniumHost)) {
                    return seleniumHost;
                } else {
                    if (isEnableSauceConnect()) {
                        return "localhost";
                    } else {
                        return "ondemand.saucelabs.com";
                    }
                }
            }



            private String getStartingURL() {
                if (getSeleniumInformation() != null) {
                    return getSeleniumInformation().getStartingURL();
                }
                return null;
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (tunnels != null) {
                    listener.getLogger().println("Shutting down Sauce OnDemand SSH tunnels");
                    tunnels.close(listener);
                }
                return true;
            }
        };
    }

    private int getPort() {
        if (seleniumPort > 0) {
            return seleniumPort;
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

    public String getSeleniumHost() {
        return seleniumHost;
    }

    public void setSeleniumHost(String seleniumHost) {
        this.seleniumHost = seleniumHost;
    }

    public int getSeleniumPort() {
        return seleniumPort;
    }

    public void setSeleniumPort(int seleniumPort) {
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

    private interface ITunnelHolder {
        void close(TaskListener listener);
    }

    private static final class TunnelHolder implements ITunnelHolder, Serializable {

        private List<SauceTunnelManager> tunnelManagers = new ArrayList<SauceTunnelManager>();
        private String buildName;
        private String username;

        public TunnelHolder(String buildName, String username) {
            this.buildName = buildName;
            this.username = username;
        }

        public Object writeReplace() {
            if (Channel.current() == null) {
                return this;
            } else {
                return Channel.current().export(ITunnelHolder.class, this);
            }
        }

        public void close(TaskListener listener) {
            for (SauceTunnelManager tunnelManager : tunnelManagers) {
                tunnelManager.closeTunnelsForPlan(username, buildName);
            }

        }
    }

    private final class SauceConnectStarter implements Callable<ITunnelHolder, IOException> {
        private String username;
        private String key;
        private String buildName;
        private BuildListener listener;
        private File sauceConnectJar;
        private int port;

        public SauceConnectStarter(String buildName, BuildListener listener, int port) {
            if (getCredentials() != null) {
                this.username = getCredentials().getUsername();
                this.key = getCredentials().getApiKey();
            } else {
                PluginImpl p = PluginImpl.get();
                this.username = p.getUsername();
                this.key = Secret.toString(p.getApiKey());
            }
            this.buildName = buildName;
            this.listener = listener;
            this.port = port;
        }

        public SauceConnectStarter(String buildName, BuildListener listener, int port, File sauceConnectJar) {
            this(buildName, listener, port);
            this.sauceConnectJar = sauceConnectJar;

        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder tunnelHolder = new TunnelHolder(buildName, username);
            SauceTunnelManager tunnelManager = new SauceConnectTwoManager();
            tunnelManager.setSauceConnectJar(sauceConnectJar);
            tunnelManager.setPrintStream(listener.getLogger());
            Object process = tunnelManager.openConnection(username, key, port);
            tunnelManager.addTunnelToMap(buildName, process);
            tunnelHolder.tunnelManagers.add(tunnelManager);
            return tunnelHolder;
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Support";
        }
    }
}
