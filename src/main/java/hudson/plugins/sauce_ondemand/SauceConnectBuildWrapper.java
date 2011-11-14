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
import java.util.List;
import java.util.Map;

/**
 * @author Ross Rowe
 */
public class SauceConnectBuildWrapper extends BuildWrapper implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<Tunnel> tunnels;

    @DataBoundConstructor
    public SauceConnectBuildWrapper(List<Tunnel> tunnels) {
        this.tunnels = Util.fixNull(tunnels);
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
        final String autoRemoteHostName = "hudson-" + Util.getDigestOf(build.getFullDisplayName()) + ".hudson";
        final ITunnelHolder tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(Util.getDigestOf(build.getFullDisplayName()), autoRemoteHostName));

        return new Environment() {
            /**
             * If the user wants automatic host name allocation, we expose that via an environment variable.
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                if (hasAutoRemoteHost()) {
                    env.put("SAUCE_ONDEMAND_HOST", autoRemoteHostName);
                    env.put("SELENIUM_STARTING_URL", "http://" + autoRemoteHostName + ':' + getPort() + '/');
                }
            }

            private int getPort() {
                for (Tunnel t : SauceConnectBuildWrapper.this.tunnels)
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

        public SauceConnectStarter(String buildName, String domain) {
            PluginImpl p = PluginImpl.get();
            this.username = p.getUsername();
            this.key = Secret.toString(p.getApiKey());
            this.domain = domain;
            this.buildName = buildName;
        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder r = new TunnelHolder(buildName);


            for (Tunnel tunnel : tunnels) {
                SauceTunnelManager tunnelManager = new SauceConnectTwoManager();
                Object process = tunnelManager.openConnection(username, key, tunnel.localHost, tunnel.localPort, tunnel.remotePort, domain);
                tunnelManager.addTunnelToMap(buildName, process);
                r.tunnelManagers.add(tunnelManager);
            }
            return null;
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
