package hudson.plugins.sauce_ondemand;

import hudson.Extension;
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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reimplementation of {@link hudson.maven.MavenTestDataPublisher} in order to add
 * a {@link SauceOnDemandReportPublisher} instance into the test report data, so that
 * embedded Sauce reports can be displayed on the test results page.
 *
 * @author Ross Rowe
 * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-11721">JENKINS-11721</a>
 */
public class SauceOnDemandTestPublisher extends Recorder {
    private final DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers;

    public SauceOnDemandTestPublisher(DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers) {
        super();
        this.testDataPublishers = testDataPublishers;
    }


    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        addTestDataPublishersToMavenModules(build, launcher, listener);
        addTestDataPublishersToBuildReport(build, launcher, listener);
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
            SauceOnDemandReportPublisher saucePublisher = new SauceOnDemandReportPublisher();
            TestResultAction.Data d = saucePublisher.getTestData(build, launcher, listener, report.getResult());
            data.add(d);

            report.setData(data);
            build.save();
        } else {
            //no test publisher defined, process stdout only
            SauceOnDemandReportPublisher saucePublisher = new SauceOnDemandReportPublisher();
            saucePublisher.getTestData(build, launcher, listener, null);
        }
    }

    public DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> getTestDataPublishers() {
        return testDataPublishers;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "Run Sauce Labs Test Publisher";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return !TestDataPublisher.all().isEmpty();
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers =
                    new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP);
            try {
                testDataPublishers.rebuild(req, formData, TestDataPublisher.all());
            } catch (IOException e) {
                throw new Descriptor.FormException(e, null);
            }
            return new SauceOnDemandTestPublisher(testDataPublishers);
        }
    }
}
