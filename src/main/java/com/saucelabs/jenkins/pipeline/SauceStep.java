package com.saucelabs.jenkins.pipeline;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Set;

@ExportedBean
public class SauceStep extends AbstractStepImpl {
    private String credentialsId;

    @DataBoundConstructor
    public SauceStep(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;

        @Inject(optional=true) private transient SauceStep step;
        @StepContextParameter private transient Run<?,?> run;

        private BodyExecution body;

        @Override public boolean start() throws Exception {
            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception(job + " must be a top-level job");
            }

            SauceCredentials credentials = SauceCredentials.getCredentialsById(job, step.getCredentialsId());
            if (credentials == null) {
                throw new Exception("no credentials provided");
            }

            HashMap<String,String> overrides = new HashMap<String,String>();
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_USERNAME, credentials.getUsername());
            overrides.put(SauceOnDemandBuildWrapper.SAUCE_ACCESS_KEY, credentials.getPassword().getPlainText());

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
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
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

        @SuppressWarnings("unchecked")
        @Override
        public Set<Class<?>> getProvidedContext() {
            return ImmutableSet.<Class<?>>of(SauceCredentials.class);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item project) {
            return new StandardUsernameListBoxModel()
                .withAll(SauceCredentials.all(project));
        }

    }
}
