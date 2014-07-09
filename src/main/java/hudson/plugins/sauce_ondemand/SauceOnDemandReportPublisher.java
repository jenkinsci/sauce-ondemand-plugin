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

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Associates Sauce OnDemand session ID to unit tests.
 *
 * @author Kohsuke Kawaguchi
 * @author Ross Rowe
 */
public class SauceOnDemandReportPublisher extends TestDataPublisher {

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceOnDemandReportPublisher.class.getName());

    /**
     * Regex which identifies the first word of the job name.
     */
    private static final String JOB_NAME_PATTERN = "\\b({0})\\b";

    /**
     * Constructs a new instance.
     */
    @DataBoundConstructor
    public SauceOnDemandReportPublisher() {
    }

    /**
     * {@inheritDoc}
     *
     * @param build         The build in progress
     * @param launcher      This launcher can be used to launch processes for this build.
     * @param buildListener Can be used to send any message.
     * @param testResult    Contains the test results for the build.
     * @return a singleton {@link SauceOnDemandReportFactory} instance if the build has Sauce results, null if no results are found
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public SauceOnDemandReportFactory getTestData(AbstractBuild<?, ?> build, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {

        SauceOnDemandBuildAction buildAction = getBuildAction(build);
        processBuildOutput(build, buildAction, testResult);
        if (buildAction.hasSauceOnDemandResults()) {
            return SauceOnDemandReportFactory.INSTANCE;
        } else {
            buildListener.getLogger().println("The Sauce OnDemand plugin is configured, but no session IDs were found in the test output.");
            return null;
        }
    }

    /**
     * Processes the build output to associate the Jenkins build with the Sauce job.
     *
     * @param build       The build in progress
     * @param buildAction the Sauce Build Action instance for the build
     * @param testResult  Contains the test results for the build.
     */
    private void processBuildOutput(AbstractBuild build, SauceOnDemandBuildAction buildAction, TestResult testResult) {
        SauceREST sauceREST = getSauceREST(buildAction);
        SauceOnDemandBuildWrapper.SauceOnDemandLogParser logParser = buildAction.getLogParser();
        if (logParser == null) {
            logger.log(Level.WARNING, "Log Parser Map did not contain " + build.toString() + ", not processing build output");
            return;
        }

        //process the stdout for the build
        String[] array = logParser.getLines().toArray(new String[logParser.getLines().size()]);
        buildAction.processSessionIds(null, array);

        //try the stdout for the tests
        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                buildAction.processSessionIds(cr, sr.getStdout(), cr.getStdout(), cr.getStdout(), cr.getStderr());
            }
        }

        for (JobInformation jobInformation : buildAction.getJobs()) {
            Map<String, Object> updates = new HashMap<String, Object>();
            //only store passed/name values if they haven't already been set
            if (jobInformation.getStatus() == null) {
                Boolean buildResult = hasTestPassed(testResult, jobInformation);
                if (buildResult != null) {
                    //set the status to passed if the test was successful
                    jobInformation.setStatus(buildResult.booleanValue() ? "passed" : "failed");
                    updates.put("passed", buildResult);
                }
            }
            if (!jobInformation.isHasJobName() && jobInformation.getName() != null) {
                updates.put("name", jobInformation.getName());
            }
            //TODO should we make the setting of the public status configurable?
//            if (!PluginImpl.get().isDisableStatusColumn()) {
            updates.put("public", true);
//            }
            if (!jobInformation.isHasBuildNumber()) {
                updates.put("build", SauceOnDemandBuildWrapper.sanitiseBuildNumber(build.toString()));
            }
            if (!updates.isEmpty()) {
                logger.fine("Performing Sauce REST update for " + jobInformation.getJobId());
                sauceREST.updateJobInfo(jobInformation.getJobId(), updates);
            }
        }
    }

    protected SauceREST getSauceREST(SauceOnDemandBuildAction buildAction) {
        return new JenkinsSauceREST(buildAction.getUsername(), buildAction.getAccessKey());
    }

    /**
     * @param testResult Contains the test results for the build.
     * @param job        details of a Sauce job which was run during the build.
     * @return Boolean indicating whether the test was successful.
     */
    private Boolean hasTestPassed(TestResult testResult, JobInformation job) {

        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                //if job name matches test class/test name, and pass/fail status is null, then populate the Sauce job with the test result status
                if (job.getName() != null && job.getStatus() == null) {
                    Pattern jobNamePattern = Pattern.compile(MessageFormat.format(JOB_NAME_PATTERN, job.getName()));
                    Matcher matcher = jobNamePattern.matcher(cr.getFullName());
                    if (job.getName().equals(cr.getFullName()) //if job name equals full name of test
                            || job.getName().contains(cr.getDisplayName()) //or if job name contains the test name
                            || matcher.find()) { //or if the full name of the test contains the job name (matching whole words only)
                        //then we have a match
                        //check the pass/fail status of the
                        return cr.getStatus().equals(CaseResult.Status.PASSED) ||
                                cr.getStatus().equals(CaseResult.Status.FIXED);
                    }

                }
            }
        }
        return null;
    }

    /**
     * @param build The build in progress
     * @return the {@link SauceOnDemandBuildAction} instance which has been registered with the build via the {@link SauceOnDemandBuildWrapper#processBuildOutput(hudson.model.AbstractBuild)} method.
     *         Can be null
     */
    private SauceOnDemandBuildAction getBuildAction(AbstractBuild<?, ?> build) {
        SauceOnDemandBuildAction buildAction = build.getAction(SauceOnDemandBuildAction.class);
        if (buildAction == null && build instanceof MavenBuild) {
            //try the parent
            buildAction = ((MavenBuild) build).getParentBuild().getAction(SauceOnDemandBuildAction.class);
        }
        return buildAction;
    }

    /**
     * Descriptor for the custom publisher.
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
        @Override
        public String getDisplayName() {
            return "Embed Sauce OnDemand reports";
        }
    }
}
