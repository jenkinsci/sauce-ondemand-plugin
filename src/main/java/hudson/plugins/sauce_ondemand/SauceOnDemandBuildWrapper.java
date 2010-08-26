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

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.SauceTunnel;
import com.saucelabs.rest.SauceTunnelFactory;
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
import hudson.util.IOException2;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.*;
import static java.util.Collections.*;

/**
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel.
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandBuildWrapper extends BuildWrapper implements Serializable {
    /**
     * Tunnel configuration.
     */
    private List<Tunnel> tunnels;

    @DataBoundConstructor
    public SauceOnDemandBuildWrapper(List<Tunnel> tunnels) {
        this.tunnels = Util.fixNull(tunnels);
    }

    public SauceOnDemandBuildWrapper(Tunnel... tunnels) {
        this(asList(tunnels));
    }

    public List<Tunnel> getTunnels() {
        return Collections.unmodifiableList(tunnels);
    }

    private boolean hasAutoRemoteHost() {
        for (Tunnel t : tunnels)
            if (t.isAutoRemoteHost())
                return true;
        return false;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting Sauce OnDemand SSH tunnels");
        final String autoRemoteHostName = "hudson-"+Util.getDigestOf(build.getFullDisplayName())+".hudson";
        final ITunnelHolder tunnels = Computer.currentComputer().getChannel().call(new TunnelStarter(autoRemoteHostName));

        return new Environment() {
            /**
             * If the user wants automatic host name allocation, we expose that via an environment variable.
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                if (hasAutoRemoteHost()) {
                    env.put("SAUCE_ONDEMAND_HOST",autoRemoteHostName);
                    env.put("SELENIUM_STARTING_URL","http://"+autoRemoteHostName+':'+getPort()+'/');
                }
            }

            private int getPort() {
                for (Tunnel t : SauceOnDemandBuildWrapper.this.tunnels)
                    return t.remotePort;
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

    private interface ITunnelHolder {
        public void close(TaskListener listener);
    }

    private static final class TunnelHolder implements ITunnelHolder, Serializable {
        final List<SauceTunnel> tunnels = new ArrayList<SauceTunnel>();

        public Object writeReplace() {
            return Channel.current().export(ITunnelHolder.class,this);
        }

        public void close(TaskListener listener) {
            for (SauceTunnel tunnel : tunnels) {
                try {
                    tunnel.disconnectAll();
                    tunnel.destroy();
                } catch (IOException e) {
                    e.printStackTrace(listener.error("Failed to shut down a tunnel"));
                }
            }
        }
    }

    private final class TunnelStarter implements Callable<ITunnelHolder,IOException> {
        private final String username, key;
        private final int timeout = TIMEOUT;
        private String autoRemoteHostName;

        private TunnelStarter(String randomHostName) {
            PluginImpl p = PluginImpl.get();
            this.username = p.getUsername();
            this.key = Secret.toString(p.getApiKey());
            this.autoRemoteHostName = randomHostName;
        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder r = new TunnelHolder();

            boolean success = false;

            try {
                SauceTunnelFactory stf = new SauceTunnelFactory(new Credential(username, key));
                for (Tunnel tunnel : tunnels) {
                    List<String> domains;
                    if (tunnel.isAutoRemoteHost()) {
                        domains = singletonList(autoRemoteHostName);
                    } else {
                        domains = tunnel.getDomainList();
                    }
                    SauceTunnel t = stf.create(domains);
                    r.tunnels.add(t);
                }
                for (int i = 0; i < tunnels.size(); i++) {
                    Tunnel s = tunnels.get(i);
                    SauceTunnel t = r.tunnels.get(i);
                    try {
                        t.waitUntilRunning(timeout);
                        if (!t.isRunning())
                            throw new IOException("Tunnel didn't come online. Aborting.");
                    } catch (InterruptedException e) {
                        throw new IOException2("Aborted",e);
                    }
                    t.connect(s.remotePort, s.localHost, s.localPort);
                }
                success = true;
            } finally {
                if (!success) {
                    // if the tunnel set up failed, revert the ones that are already created
                    for (SauceTunnel t : r.tunnels) {
                        t.destroy();
                    }
                }
            }
            return r;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand SSH tunnel";
        }
    }

    /**
     * Time out for how long we wait until the tunnel to be set up.
     */
    public static int TIMEOUT = Integer.getInteger(SauceOnDemandBuildWrapper.class.getName()+".timeout", 180 * 1000);

    private static final long serialVersionUID = 1L;
}
