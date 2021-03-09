package com.saucelabs.jenkins.pipeline;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.plugins.sauce_ondemand.SauceEnvironmentUtil;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildAction;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

@ExportedBean
public class SauceStep extends Step {
    private final String credentialsId;

    @DataBoundConstructor
    public SauceStep(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, credentialsId);
    }

    public static class Execution extends StepExecution {
        private static final long serialVersionUID = 1;

        private final String credentialsId;

        public Execution(@Nonnull StepContext context, String credentialsId) {
            super(context);
            this.credentialsId = credentialsId;
        }

        private BodyExecution body;

        @Override public boolean start() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);

            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception(job + " must be a top-level job");
            }

            SauceCredentials credentials = SauceCredentials.getCredentialsById(job, credentialsId);
            if (credentials == null) {
                throw new Exception("no credentials provided");
            }
            CredentialsProvider.track(run, credentials);


            HashMap<String,String> overrides = new HashMap<String,String>();
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_USERNAME, credentials.getUsername());
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_ACCESS_KEY, credentials.getPassword().getPlainText());
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_REST_ENDPOINT, credentials.getRestEndpoint());
            overrides.put(SauceOnDemandBuildWrapper.JENKINS_BUILD_NUMBER, SauceEnvironmentUtil.getSanitizedBuildNumber(run));
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_BUILD_NAME, SauceEnvironmentUtil.getSanitizedBuildNumber(run));

            SauceOnDemandBuildAction buildAction = run.getAction(SauceOnDemandBuildAction.class);
            if (buildAction == null) {
                buildAction = new SauceOnDemandBuildAction(run, credentials.getId());
                run.addAction(buildAction);
            }

            body = getContext().newBodyInvoker()
                .withContext(credentials)
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overrides)))
                .withCallback(BodyExecutionCallback.wrap(getContext()))
                .start();
            return false;
        }

        @Override public void stop(@Nonnull Throwable cause) throws Exception {
            // should be no need to do anything special (but verify in JENKINS-26148)
            if (body!=null) {
                body.cancel(cause);
            }
        }

    }


    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class);
        }

        @Override public String getDisplayName() {
            return "Sauce";
        }

        @Override public String getFunctionName() {
            return "sauce";
        }

        @Override public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<Class<?>> getProvidedContext() {
            return Collections.<Class<?>>singleton(SauceCredentials.class);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item project) {
            return new StandardUsernameListBoxModel()
                .withAll(SauceCredentials.all(project));
        }

    }
}
