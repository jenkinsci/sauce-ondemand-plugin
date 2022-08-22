package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResultAction;
import hudson.util.DescribableList;
import jenkins.tasks.SimpleBuildStep;
import org.json.JSONObject;
import org.json.JSONException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;

/**
 * Reimplementation of {@link hudson.maven.MavenTestDataPublisher} in order to add
 * a {@link SauceOnDemandReportPublisher} instance into the test report data, so that
 * embedded Sauce reports can be displayed on the test results page.
 *
 * @author Ross Rowe
 * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-11721">JENKINS-11721</a>
 */
public class SauceOnDemandTestPublisher extends Recorder implements SimpleBuildStep {
    private DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP);;
    private JSONObject mixpanelJSON;

    @DataBoundConstructor
    public SauceOnDemandTestPublisher() {
        super();
    }

    public SauceOnDemandTestPublisher(DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers) {
        super();
        this.testDataPublishers = testDataPublishers;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * {@inheritDoc}
     *
     * Created for implementing SimpleBuildStep / pipeline
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        TestResultAction report = run.getAction(TestResultAction.class);
        if (report != null) {
            List<TestResultAction.Data> data = new ArrayList<TestResultAction.Data>();
            if (testDataPublishers != null) {
                for (TestDataPublisher tdp : testDataPublishers) {
                    TestResultAction.Data d = tdp.contributeTestData(run, workspace, launcher, listener, report.getResult());
                    if (d != null) {
                        data.add(d);
                    }
                }
            }
            SauceOnDemandReportPublisher saucePublisher = createReportPublisher();
            TestResultAction.Data d = saucePublisher.contributeTestData(run, workspace, launcher, listener, report.getResult());
            data.add(d);

            report.setData(data);
            run.save();
        } else {
            //no test publisher defined, process stdout only
            SauceOnDemandReportPublisher saucePublisher = createReportPublisher();
            saucePublisher.contributeTestData(run, workspace, launcher, listener, null);
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        addTestDataPublishersToMavenModules(build, launcher, listener);
        addTestDataPublishersToBuildReport(build, launcher, listener);

        // send mixpanel outside of the build processing above so we don't end up sending multiple messages per build
        if (isDisableUsageStats()) {
            return true;
        }
        if (mixpanelJSON == null) {
            return true;
        }
        try {
            MessageBuilder messageBuilder = new MessageBuilder("5d9a83c5f58311b7b88622d0da5e7e9d");
            JSONObject sentEvent = messageBuilder.event(mixpanelJSON.getString("username"), "Jenkins JUNIT status", mixpanelJSON);
            ClientDelivery delivery = new ClientDelivery();
            delivery.addMessage(sentEvent);
            MixpanelAPI mixpanel = new MixpanelAPI();
            mixpanel.deliver(delivery);
        } catch (JSONException e) {
            listener.getLogger().println("Could not send junit status: " + e.getMessage());
        }

        return true;
    }

    private void addTestDataPublishersToMavenModules(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {
        if (build instanceof MavenModuleSetBuild) {
            Map<MavenModule, List<MavenBuild>> buildsMap = ((MavenModuleSetBuild) build).getModuleBuilds();
            for (List<MavenBuild> mavenBuild : buildsMap.values()) {
                MavenBuild lastBuild = getLastOrNullIfEmpty(mavenBuild);
                if (lastBuild != null) {
                    addTestDataPublishersToBuildReport(lastBuild, launcher, listener);
                }
            }
        }
    }

    private MavenBuild getLastOrNullIfEmpty(List<MavenBuild> builds) {
        if (builds.isEmpty()) {
            return null;
        } else {
            return builds.get(builds.size() - 1);
        }
    }

    /**
    * Keep a copy of the mixpanel json and specifically whether or not we've sent the failure message
    */
    private void updateMixpanelJSON(JSONObject mixpanelJSON) {
        try {
            if (mixpanelJSON == null) {
                return;
            }
            if (this.mixpanelJSON == null) {
                this.mixpanelJSON = mixpanelJSON;
                return;
            }
            if (this.mixpanelJSON.getBoolean("failureMessageSent")) {
                return;
            }
            this.mixpanelJSON.put("failureMessageSent", mixpanelJSON.getBoolean("failureMessageSent"));
        } catch (JSONException e) {
        }

    }

    private boolean isDisableUsageStats() {
        PluginImpl plugin = PluginImpl.get();
        if (plugin == null) { return true; }
        return plugin.isDisableUsageStats();
    }

    private void addTestDataPublishersToBuildReport(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {
        TestResultAction report = build.getAction(TestResultAction.class);
        if (report != null) {
            List<TestResultAction.Data> data = new ArrayList<TestResultAction.Data>();
            if (testDataPublishers != null) {
                for (TestDataPublisher tdp : testDataPublishers) {
                    TestResultAction.Data d = tdp.getTestData(build, launcher, listener, report.getResult());
                    if (d != null) {
                        data.add(d);
                    }
                }
            }
            SauceOnDemandReportPublisher saucePublisher = createReportPublisher();
            TestResultAction.Data d = saucePublisher.getTestData(build, launcher, listener, report.getResult());
            updateMixpanelJSON(saucePublisher.getMixpanelJSON());
            data.add(d);

            report.setData(data);
            build.save();
        } else {
            //no test publisher defined, process stdout only
            SauceOnDemandReportPublisher saucePublisher = createReportPublisher();
            saucePublisher.getTestData(build, launcher, listener, null);
            updateMixpanelJSON(saucePublisher.getMixpanelJSON());
        }
    }

    protected SauceOnDemandReportPublisher createReportPublisher() {
        return new SauceOnDemandReportPublisher();
    }

    public List<TestDataPublisher> getTestDataPublishers() {
        return testDataPublishers == null ? Collections.<TestDataPublisher>emptyList() : testDataPublishers;
    }

    /*@DataBoundSetter
    public void setTestDataPublishers(ArrayList testDataPublishers) {
        // This should never actually be called, but is needed because the pipeline generator provides a list
        this.testDataPublishers = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(
            Saveable.NOOP,
            testDataPublishers
        );
    }*/

    @DataBoundSetter public final void setTestDataPublishers(@NonNull List<TestDataPublisher> testDataPublishers) {
        this.testDataPublishers = new DescribableList<TestDataPublisher,Descriptor<TestDataPublisher>>(Saveable.NOOP);
        this.testDataPublishers.addAll(testDataPublishers);
    }

    @Extension
    @Symbol("saucePublisher")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "Run Sauce Labs Test Publisher";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return !TestDataPublisher.all().isEmpty();
        }
    }
}
