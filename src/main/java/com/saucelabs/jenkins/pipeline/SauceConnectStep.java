package com.saucelabs.jenkins.pipeline;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.saucelabs.ci.sauceconnect.AbstractSauceTunnelManager;
import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import com.saucelabs.saucerest.DataCenter;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.plugins.sauce_ondemand.PluginImpl;
import hudson.plugins.sauce_ondemand.SauceEnvironmentUtil;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.ListBoxModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class SauceConnectStep extends Step {
    private Boolean verboseLogging = false;
    private Boolean useLatestSauceConnect = false;
    private Boolean useGeneratedTunnelIdentifier = false;
    private String options;
    private String optionsSC5;
    private String sauceConnectPath;

    @DataBoundConstructor
    public SauceConnectStep() {
    }

    public SauceConnectStep(String options, String optionsSC5, Boolean verboseLogging, Boolean useLatestSauceConnect, Boolean useGeneratedTunnelIdentifier, String sauceConnectPath) {
        this.verboseLogging = verboseLogging;
        this.useLatestSauceConnect = useLatestSauceConnect;
        this.useGeneratedTunnelIdentifier = useGeneratedTunnelIdentifier;
        this.sauceConnectPath = Util.fixEmptyAndTrim(sauceConnectPath);
        this.options = StringUtils.trimToEmpty(options);
        this.optionsSC5 = StringUtils.trimToEmpty(optionsSC5);
    }

    public static SauceConnectManager getSauceTunnelManager() {
        return HudsonSauceManagerFactory.getInstance().createSauceConnectManager();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SauceConnectStepExecution(context,
            PluginImpl.get().getSauceConnectOptions(),
            options,
            optionsSC5,
            useGeneratedTunnelIdentifier,
            verboseLogging,
            sauceConnectPath,
            useLatestSauceConnect,
            Jenkins.get().getProxy()
        );
    }


    public String getOptions() {
        return options;
    }

    @DataBoundSetter
    public void setOptions(String options) {
        this.options = options.strip();
    }

    public String getOptionsSC5() {
        return optionsSC5;
    }

    @DataBoundSetter
    public void setOptionsSC5(String optionsSC5) {
        this.optionsSC5 = optionsSC5.strip();
    }

    public String getSauceConnectPath() {
        return sauceConnectPath;
    }

    @DataBoundSetter
    public void setSauceConnectPath(String sauceConnectPath) {
        this.sauceConnectPath = sauceConnectPath;
    }

    public Boolean getUseGeneratedTunnelIdentifier() {
        return useGeneratedTunnelIdentifier;
    }

    @DataBoundSetter
    public void setUseGeneratedTunnelIdentifier(Boolean useGeneratedTunnelIdentifier) {
        this.useGeneratedTunnelIdentifier = useGeneratedTunnelIdentifier;
    }

    public Boolean getUseLatestSauceConnect() {
        return useLatestSauceConnect;
    }

    public Boolean getVerboseLogging() {
        return verboseLogging;
    }

    @DataBoundSetter
    public void setVerboseLogging(Boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }


    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override public String getDisplayName() {
            return "Sauce Connect";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, Run.class, Computer.class, TaskListener.class, SauceCredentials.class);
            return Collections.unmodifiableSet(context);
        }

        @Override public String getFunctionName() {
            return "sauceconnect";
        }

        @Override public boolean takesImplicitBlockArgument() {
            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item project) {
            return new StandardUsernameListBoxModel()
                .withAll(SauceCredentials.all(project));
        }

    }

    private static final class SauceStartConnectHandler extends MasterToSlaveCallable<Void, AbstractSauceTunnelManager.SauceConnectException> {
        private final SauceCredentials sauceCredentials;
        private final int port;
        private final String options;
        private final TaskListener listener;
        private final Boolean verboseLogging;
        private final String sauceConnectPath;
        private final Boolean useLatestSauceConnect;
        private final Boolean legacyCLI;
        private final ProxyConfiguration proxy;

        SauceStartConnectHandler(SauceCredentials sauceCredentials, int port, String options, TaskListener listener, Boolean verboseLogging, String sauceConnectPath, Boolean useLatestSauceConnect, Boolean legacyCLI, ProxyConfiguration proxy) {
            this.sauceCredentials = sauceCredentials;
            this.port = port;
            this.options = options.strip();
            this.listener = listener;
            this.verboseLogging = verboseLogging;
            this.sauceConnectPath = sauceConnectPath;
            this.useLatestSauceConnect = useLatestSauceConnect;
            this.legacyCLI = legacyCLI;
            this.proxy = proxy;
        }

        @Override
        public Void call() throws AbstractSauceTunnelManager.SauceConnectException {
            SauceConnectManager sauceTunnelManager = getSauceTunnelManager();
            sauceTunnelManager.setSauceRest(sauceCredentials.getSauceREST(proxy));
            sauceTunnelManager.setUseLatestSauceConnect(useLatestSauceConnect);
            sauceTunnelManager.openConnection(
                sauceCredentials.getUsername(),
                sauceCredentials.getApiKey().getPlainText(),
                DataCenter.fromString(sauceCredentials.getRestEndpointName()),
                port,
                null, /*sauceConnectJar,*/
                options,
                listener.getLogger(),
                verboseLogging,
                sauceConnectPath,
                legacyCLI
            );
            return null;
        }
    }


    private static final class SauceStopConnectHandler extends MasterToSlaveCallable<Void, AbstractSauceTunnelManager.SauceConnectException> {
        private final SauceCredentials sauceCredentials;
        private final String options;
        private final TaskListener listener;
        private final ProxyConfiguration proxy;

        SauceStopConnectHandler(SauceCredentials sauceCredentials, String options, TaskListener listener, ProxyConfiguration proxy) {
            this.sauceCredentials = sauceCredentials;
            this.options = options;
            this.listener = listener;
            this.proxy = proxy;
        }

        @Override
        public Void call() throws AbstractSauceTunnelManager.SauceConnectException {
            SauceConnectManager sauceTunnelManager = getSauceTunnelManager();
            sauceTunnelManager.setSauceRest(sauceCredentials.getSauceREST(proxy));
            sauceTunnelManager.closeTunnelsForPlan(
                sauceCredentials.getUsername(),
                options,
                listener.getLogger()
            );
            return null;
        }
    }

    public static class SauceConnectStepExecution extends StepExecution {
        private final String globalOptions;
        private final String options;
        private final String optionsSC5;
        private final boolean useGeneratedTunnelIdentifier;
        private final boolean verboseLogging;
        private final String sauceConnectPath;
        private final boolean useLatestSauceConnect;

        private static final long serialVersionUID = 1L;
        private final ProxyConfiguration proxy;

        private BodyExecution body;

        public SauceConnectStepExecution(
            @NonNull StepContext context,
            String globalOptions,
            String options,
            String optionsSC5,
            boolean useGeneratedTunnelIdentifier,
            boolean verboseLogging,
            String sauceConnectPath,
            boolean useLatestSauceConnect,
            ProxyConfiguration proxy
        ) {
            super(context);
            this.globalOptions = globalOptions;
            this.options = options;
            this.optionsSC5 = optionsSC5;
            this.useGeneratedTunnelIdentifier = useGeneratedTunnelIdentifier;
            this.verboseLogging = verboseLogging;
            this.sauceConnectPath = sauceConnectPath;
            this.useLatestSauceConnect = useLatestSauceConnect;
            this.proxy = proxy;
        }

        @Override
        public boolean start() throws Exception {
            boolean legacyCLI = false;
            if (options != null && optionsSC5 != null && !options.isEmpty() && !optionsSC5.isEmpty()) {
                throw new Exception("Legacy and SC5 CLI options cannot both be specified");
            }

            if (options != null && !options.isEmpty()) {
                legacyCLI = true;
            }

            Run<?, ?> run = getContext().get(Run.class);
            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception(job + " must be a top-level job");
            }
            Computer computer = getContext().get(Computer.class);
            Node node = computer.getNode();
            if (node == null) {
                throw new Exception("computer does not correspond to a live node");
            }
            int port = computer.getChannel().call(
                new SauceOnDemandBuildWrapper.GetAvailablePort()
            );

            ArrayList<String> optionsArray = new ArrayList<String>();
            optionsArray.add(globalOptions);
            if (legacyCLI) {
                optionsArray.add(options);
            } else {
                optionsArray.add(optionsSC5);
            }
            optionsArray.removeAll(Collections.singleton("")); // remove the empty strings

            String combinedOptions = StringUtils.join(optionsArray, " ");

            HashMap<String,String> overrides = new HashMap<String,String>();
            overrides.put(SauceOnDemandBuildWrapper.SELENIUM_PORT, String.valueOf(port));
            overrides.put(SauceOnDemandBuildWrapper.SELENIUM_HOST, "localhost");

            if (useGeneratedTunnelIdentifier) {
                final String tunnelName = SauceEnvironmentUtil.generateTunnelName(job.getName(), run.number);
                overrides.put(SauceOnDemandBuildWrapper.TUNNEL_NAME, tunnelName);
                combinedOptions = combinedOptions + " --tunnel-name " + tunnelName;
            }

            SauceCredentials sauceCredentials = getContext().get(SauceCredentials.class);
            final String region = sauceCredentials.getRegion();
            combinedOptions = combinedOptions + " --region " + region;

            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Starting sauce connect");

            SauceStartConnectHandler handler = new SauceStartConnectHandler(
                sauceCredentials,
                port,
                combinedOptions,
                listener,
                verboseLogging,
                sauceConnectPath,
                useLatestSauceConnect,
                legacyCLI,
                proxy
            );
            computer.getChannel().call(handler);

            body = getContext().newBodyInvoker()
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overrides)))
                .withCallback(new Callback(sauceCredentials, combinedOptions, proxy))
                .withDisplayName("Sauce Connect")
                .start();

            return false;
        }

        @Override
        public void stop(@NonNull Throwable cause) throws Exception {
            if (body!=null) {
                body.cancel(cause);
            }

        }

        private static final class Callback extends BodyExecutionCallback.TailCall {

            private final String options;
            private final SauceCredentials sauceCredentials;

            private final ProxyConfiguration proxy;


            Callback(SauceCredentials sauceCredentials, String options, ProxyConfiguration proxy) {
                this.sauceCredentials = sauceCredentials;
                this.options = options;
                this.proxy = proxy;
            }

            @Override protected void finished(StepContext context) throws Exception {
                TaskListener listener = context.get(TaskListener.class);
                Computer computer = context.get(Computer.class);

                SauceStopConnectHandler stopConnectHandler = new SauceStopConnectHandler(
                    sauceCredentials,
                    options,
                    listener,
                    proxy
                );
                computer.getChannel().call(stopConnectHandler);
            }

        }
    }
}