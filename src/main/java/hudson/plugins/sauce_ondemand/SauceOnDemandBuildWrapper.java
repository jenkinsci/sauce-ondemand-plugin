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
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static scala.actors.threadpool.Arrays.asList;

/**
 * {@link BuildWrapper} that sets up the Sauce OnDemand SSH tunnel.
 *
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
        
        String buildNameDigest = Util.getDigestOf(build.getFullDisplayName());
        final ITunnelHolder tunnels;
        if (!(Computer.currentComputer() instanceof Hudson.MasterComputer)) {
            File sauceConnectJar = copySauceConnectToSlave(build, listener);
            tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(buildNameDigest, listener, sauceConnectJar));
        } else {
            tunnels = Computer.currentComputer().getChannel().call(new SauceConnectStarter(buildNameDigest, listener));
        }


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

    private File copySauceConnectToSlave(AbstractBuild build, BuildListener listener) throws IOException {
        class CopyImpl extends Copy {
            private int copySize;

            public CopyImpl() {
                setProject(new org.apache.tools.ant.Project());
            }

            @Override
            protected void doFileOperations() {
                copySize = super.fileCopyMap.size();
                super.doFileOperations();
            }

            public int getNumCopied() {
                return copySize;
            }
        }

        FilePath projectWorkspaceOnSlave = build.getProject().getWorkspace();
        try {
            File sauceConnectJar = SauceConnectUtils.extractSauceConnectJarFile();
            FileSet fs = Util.createFileSet(sauceConnectJar.getParentFile(), sauceConnectJar.getName(), "");
            CopyImpl copyTask = new CopyImpl();
            copyTask.setTodir(new File(projectWorkspaceOnSlave.getRemote()));
            copyTask.addFileset(fs);
            copyTask.setOverwrite(true);
            copyTask.setIncludeEmptyDirs(false);
            copyTask.setFlatten(false);

            copyTask.execute();
            return new File(projectWorkspaceOnSlave.getRemote(), sauceConnectJar.getName());
        } catch (URISyntaxException e) {
            listener.error("Error copying sauce connect jar to slave", e);
        }
        return null;
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
        private String buildName;
        private BuildListener listener;
        private File sauceConnectJar;

        public SauceConnectStarter(String buildName, BuildListener listener) {
            PluginImpl p = PluginImpl.get();
            this.username = p.getUsername();
            this.key = Secret.toString(p.getApiKey());
            this.buildName = buildName;
            this.listener = listener;
        }

        public SauceConnectStarter(String buildName, BuildListener listener, File sauceConnectJar) {
            this(buildName, listener);
            this.sauceConnectJar = sauceConnectJar;

        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder r = new TunnelHolder(buildName);

            for (Tunnel tunnel : tunnels) {
                SauceTunnelManager tunnelManager = new SauceConnectTwoManager();
                tunnelManager.setSauceConnectJar(sauceConnectJar);
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
