package com.saucelabs.jenkins.pipeline;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.google.common.collect.ImmutableSet;
import com.saucelabs.ci.sauceconnect.AbstractSauceTunnelManager;
import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import hudson.Extension;
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
import java.util.Set;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.saucelabs.jenkins.pipeline.SauceConnectStep.SauceConnectStepExecution.getSauceTunnelManager;

public class SauceConnectStep extends Step {
    private Boolean verboseLogging = false;
    private Boolean useLatestSauceConnect = false;
    private Boolean useGeneratedTunnelIdentifier = false;
    private String options;
    private String sauceConnectPath;

    @DataBoundConstructor
    public SauceConnectStep() {
    }

    public SauceConnectStep(String options, Boolean verboseLogging, Boolean useLatestSauceConnect, Boolean useGeneratedTunnelIdentifier, String sauceConnectPath) {
        this.verboseLogging = verboseLogging;
        this.useLatestSauceConnect = useLatestSauceConnect;
        this.useGeneratedTunnelIdentifier = useGeneratedTunnelIdentifier;
        this.sauceConnectPath = Util.fixEmptyAndTrim(sauceConnectPath);
        this.options = StringUtils.trimToEmpty(options);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SauceConnectStepExecution(context,
            PluginImpl.get().getSauceConnectOptions(),
            options,
            useGeneratedTunnelIdentifier,
            verboseLogging,
            sauceConnectPath,
            useLatestSauceConnect
        );
    }


    public String getOptions() {
        return options;
    }

    @DataBoundSetter
    public void setOptions(String options) {
        this.options = options;
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

    @DataBoundSetter
    public void setUseLatestSauceConnect(Boolean useLatestSauceConnect) {
        this.useLatestSauceConnect = useLatestSauceConnect;
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
            return ImmutableSet.of(Run.class, Computer.class, TaskListener.class, SauceCredentials.class);
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

        SauceStartConnectHandler(SauceCredentials sauceCredentials, int port, String options, TaskListener listener, Boolean verboseLogging, String sauceConnectPath, Boolean useLatestSauceConnect) {
            this.sauceCredentials = sauceCredentials;
            this.port = port;
            this.options = options;
            this.listener = listener;
            this.verboseLogging = verboseLogging;
            this.sauceConnectPath = sauceConnectPath;
            this.useLatestSauceConnect = useLatestSauceConnect;
        }

        @Override
        public Void call() throws AbstractSauceTunnelManager.SauceConnectException {
            SauceConnectFourManager sauceTunnelManager = getSauceTunnelManager();
            sauceTunnelManager.setSauceRest(sauceCredentials.getSauceREST());
            sauceTunnelManager.setUseLatestSauceConnect(useLatestSauceConnect);
            sauceTunnelManager.openConnection(
                sauceCredentials.getUsername(),
                sauceCredentials.getApiKey().getPlainText(),
                port,
                null, /*sauceConnectJar,*/
                options,
                listener.getLogger(),
                verboseLogging,
                sauceConnectPath
            );
            return null;
        }
    }


    private static final class SauceStopConnectHandler extends MasterToSlaveCallable<Void, AbstractSauceTunnelManager.SauceConnectException> {
        private final SauceCredentials sauceCredentials;
        private final String options;
        private final TaskListener listener;

        SauceStopConnectHandler(SauceCredentials sauceCredentials, String options, TaskListener listener) {
            this.sauceCredentials = sauceCredentials;
            this.options = options;
            this.listener = listener;
        }

        @Override
        public Void call() throws AbstractSauceTunnelManager.SauceConnectException {
            SauceConnectFourManager sauceTunnelManager = getSauceTunnelManager();
            sauceTunnelManager.setSauceRest(sauceCredentials.getSauceREST());
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
        private final boolean useGeneratedTunnelIdentifier;
        private final boolean verboseLogging;
        private final String sauceConnectPath;
        private final boolean useLatestSauceConnect;

        private static final long serialVersionUID = 1L;

        private BodyExecution body;

        public SauceConnectStepExecution(
            @Nonnull StepContext context,
            String globalOptions,
            String options,
            boolean useGeneratedTunnelIdentifier,
            boolean verboseLogging,
            String sauceConnectPath,
            boolean useLatestSauceConnect
        ) {
            super(context);
            this.globalOptions = globalOptions;
            this.options = options;
            this.useGeneratedTunnelIdentifier = useGeneratedTunnelIdentifier;
            this.verboseLogging = verboseLogging;
            this.sauceConnectPath = sauceConnectPath;
            this.useLatestSauceConnect = useLatestSauceConnect;
        }

        @Override
        public boolean start() throws Exception {

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
            optionsArray.add(options);
            optionsArray.removeAll(Collections.singleton("")); // remove the empty strings

            String options = StringUtils.join(optionsArray, " ");

            HashMap<String,String> overrides = new HashMap<String,String>();
            overrides.put(SauceOnDemandBuildWrapper.SELENIUM_PORT, String.valueOf(port));
            overrides.put(SauceOnDemandBuildWrapper.SELENIUM_HOST, "localhost");

            if (useGeneratedTunnelIdentifier) {
                final String tunnelIdentifier = SauceEnvironmentUtil.generateTunnelIdentifier(job.getName());
                overrides.put(SauceOnDemandBuildWrapper.TUNNEL_IDENTIFIER, tunnelIdentifier);
                options = options + " --tunnel-name " + tunnelIdentifier;

            }

            SauceCredentials sauceCredentials = getContext().get(SauceCredentials.class);
            final String restEndpoint = sauceCredentials.getRestEndpoint();

            overrides.put(SauceOnDemandBuildWrapper.SAUCE_REST_ENDPOINT, restEndpoint);
            options = options + " -x " + restEndpoint + "rest/v1";

            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Starting sauce connect");

            SauceStartConnectHandler handler = new SauceStartConnectHandler(
                sauceCredentials,
                port,
                options,
                listener,
                verboseLogging,
                sauceConnectPath,
                useLatestSauceConnect
            );
            computer.getChannel().call(handler);

            body = getContext().newBodyInvoker()
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overrides)))
                .withCallback(new Callback(sauceCredentials, options))
                .withDisplayName("Sauce Connect")
                .start();

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            if (body!=null) {
                body.cancel(cause);
            }

        }

        public static SauceConnectFourManager getSauceTunnelManager() {
            return HudsonSauceManagerFactory.getInstance().createSauceConnectFourManager();
        }

        private static final class Callback extends BodyExecutionCallback.TailCall {

            private final String options;
            private final SauceCredentials sauceCredentials;


            Callback(SauceCredentials sauceCredentials, String options) {
                this.sauceCredentials = sauceCredentials;
                this.options = options;
            }

            @Override protected void finished(StepContext context) throws Exception {
                TaskListener listener = context.get(TaskListener.class);
                Computer computer = context.get(Computer.class);

                SauceStopConnectHandler stopConnectHandler = new SauceStopConnectHandler(
                    sauceCredentials,
                    options,
                    listener
                );
                computer.getChannel().call(stopConnectHandler);
            }

        }
    }
}
