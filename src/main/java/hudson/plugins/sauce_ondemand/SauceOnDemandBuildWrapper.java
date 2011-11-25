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

import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static scala.actors.threadpool.Arrays.asList;

/**
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel.
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandBuildWrapper extends BuildWrapper implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Tunnel> tunnels;

    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(List<Tunnel> tunnels) {
        this.tunnels = Util.fixNull(tunnels);
    }

    public SauceOnDemandBuildWrapper(Tunnel... tunnels) {
        this(asList(tunnels));
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting Sauce OnDemand SSH tunnels");
        //autoRemoteHostName not currently used
        String buildNameDigest = Util.getDigestOf(build.getFullDisplayName());
        final String autoRemoteHostName = "hudson-" + buildNameDigest + ".hudson";
        final ITunnelHolder tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(buildNameDigest, autoRemoteHostName, listener));

        return new Environment() {
            /**
             * If the user wants automatic host name allocation, we expose that via an environment variable.
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("SAUCE_ONDEMAND_HOST", getHostName());
                env.put("SELENIUM_STARTING_URL", "http://" + getHostName() + ':' + getPort() + '/');
            }

            private String getHostName() {
               for (Tunnel t : SauceOnDemandBuildWrapper.this.tunnels)
                    return t.localHost;
                return "localhost";
            }


            private int getPort() {
                for (Tunnel t : SauceOnDemandBuildWrapper.this.tunnels)
                    return t.localPort;
                return 80;
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                listener.getLogger().println("Shutting down Sauce OnDemand SSH tunnels");
                tunnels.close(listener);
                return true;
            }
        };
    }

    public List<Tunnel> getTunnels() {
        return Collections.unmodifiableList(tunnels);
    }


    private interface ITunnelHolder {
        void close(TaskListener listener);
    }

    private static final class TunnelHolder implements ITunnelHolder, Serializable {

        private List<SauceTunnelManager> tunnelManagers = new ArrayList<SauceTunnelManager>();
        private String buildName;                       

        public TunnelHolder(String buildName) {
            this.buildName = buildName;
        }

        public Object writeReplace() {
            return Channel.current().export(ITunnelHolder.class, this);
        }

        public void close(TaskListener listener) {
            for (SauceTunnelManager tunnelManager : tunnelManagers) {
                tunnelManager.closeTunnelsForPlan(buildName);
            }

        }
    }

    private final class SauceConnectStarter implements Callable<ITunnelHolder, IOException> {
        private String username;
        private String key;
        private String domain;
        private String buildName;
        private BuildListener listener;

        public SauceConnectStarter(String buildName, String domain, BuildListener listener) {
            PluginImpl p = PluginImpl.get();
            this.username = p.getUsername();
            this.key = Secret.toString(p.getApiKey());
            this.domain = domain;
            this.buildName = buildName;
            this.listener = listener;
        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder r = new TunnelHolder(buildName);

            for (Tunnel tunnel : tunnels) {
                SauceTunnelManager tunnelManager = new SauceConnectTwoManager();
                tunnelManager.setPrintStream(listener.getLogger());
                Object process = tunnelManager.openConnection(username, key);
                tunnelManager.addTunnelToMap(buildName, process);
                r.tunnelManagers.add(tunnelManager);
            }
            return r;
        }
    }
    
    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        @Override
        public String getDisplayName() {
            return "Sauce Connect";
        }
    }
}
