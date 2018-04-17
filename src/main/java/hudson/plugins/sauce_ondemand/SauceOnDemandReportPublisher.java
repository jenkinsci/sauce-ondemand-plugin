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

import com.google.common.base.Strings;
import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.ListBoxModel;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

import hudson.plugins.sauce_ondemand.SauceOnDemandBuildAction;

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
     * What job security level we should set jobs to
     */
    private String jobVisibility = "";


    /**
     * Constructs a new instance.
     */
    @DataBoundConstructor
    public SauceOnDemandReportPublisher() {
    }


    public String getJobVisibility() {
        return jobVisibility;
    }

    @DataBoundSetter
    public void setJobVisibility(String jobVisibility) {
        this.jobVisibility = jobVisibility;
    }


    @Override
    public TestResultAction.Data contributeTestData(Run<?, ?> run, @Nonnull FilePath workspace, Launcher launcher, TaskListener listener, TestResult testResult) throws IOException, InterruptedException {
        try {
            listener.getLogger().println("Starting Sauce Labs test publisher");
            SauceOnDemandBuildAction buildAction = SauceOnDemandBuildAction.getSauceBuildAction(run);
            if (buildAction != null) {
                processBuildOutput(run, buildAction, testResult);
                if (buildAction.hasSauceOnDemandResults()) {
                    return SauceOnDemandReportFactory.INSTANCE;
                } else {
                    listener.getLogger().println("The Sauce OnDemand plugin is configured, but no session IDs were found in the test output.");
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // FIXME - shortterm
        } finally {
            listener.getLogger().println("Finished Sauce Labs test publisher");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param build         The build in progress
     * @param launcher      This launcher can be used to launch processes for this build.
     * @param listener Can be used to send any message.
     * @param testResult    Contains the test results for the build.
     * @return a singleton {@link SauceOnDemandReportFactory} instance if the build has Sauce results, null if no results are found
     */
    @Override
    public SauceOnDemandReportFactory getTestData(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, TestResult testResult) {
        try
        {
            listener.getLogger().println("Starting Sauce Labs test publisher");
            SauceOnDemandBuildAction buildAction = SauceOnDemandBuildAction.getSauceBuildAction(build);
            if (buildAction != null) {
                processBuildOutput(build, buildAction, testResult);
                if (buildAction.hasSauceOnDemandResults()) {
                    return SauceOnDemandReportFactory.INSTANCE;
                } else {
                    listener.getLogger().println("The Sauce OnDemand plugin is configured, but no session IDs were found in the test output.");
                    return null;
                }
            }
            return null;
        } finally {
            listener.getLogger().println("Finished Sauce Labs test publisher");
        }
    }

    /**
     * Processes the log output, and for lines which are in the valid log format, return a list that is found
     *
     * @param isStdout   is this stdout?
     * @param logStrings     lines of output to be processed, not null
     * @return list of session ids found in log strings
     */
    public static LinkedList<TestIDDetails> processSessionIds(Boolean isStdout, String... logStrings) {
        logger.log(Level.FINE, isStdout == null ? "Parsing Sauce Session ids in stdout" : "Parsing Sauce Session ids in test results");

        LinkedList<TestIDDetails> onDemandTests = new LinkedList<TestIDDetails>();

        for (String logString : logStrings) {
            if (logString == null) continue;
            for (String text : logString.split("\n|\r")) {
                TestIDDetails details = TestIDDetails.processString(text);
                if (details != null) {
                    onDemandTests.add(details);
                }
            }
        }
        return onDemandTests;
    }

    /**
     * Processes the build output to associate the Jenkins build with the Sauce job.
     *
     * @param build       The build in progress
     * @param buildAction the Sauce Build Action instance for the build
     * @param testResult  Contains the test results for the build.
     */
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private void processBuildOutput(Run build, SauceOnDemandBuildAction buildAction, TestResult testResult) {
        SauceREST sauceREST = getSauceREST(build);

        LinkedHashMap<String, JenkinsJobInformation> onDemandTests;
        List<CaseResult> failedTests;
        HashMap failedTestsMap = new HashMap();

        try {
            onDemandTests = buildAction.retrieveJobIdsFromSauce(sauceREST, build);
        } catch (JSONException e) {
            onDemandTests = new LinkedHashMap<>();

            logger.severe(e.getMessage());
        }

        LinkedList<TestIDDetails> testIds = new LinkedList<TestIDDetails>();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(build.getLogInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                testIds.addAll(processSessionIds(true, line));
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        JenkinsBuildInformation buildInformation = buildAction.getSauceBuild();

        //try the stdout for the tests, if build are aborted testResult will be null
        if (testResult != null) {
            for (SuiteResult sr : testResult.getSuites()) {
                testIds.addAll(processSessionIds(false, sr.getStdout(), sr.getStderr()));

                for (CaseResult cr : sr.getCases()) {
                    if (!Objects.equals(cr.getStdout(), sr.getStdout())) {
                        testIds.addAll(processSessionIds(false, cr.getStdout()));
                    }
                    if (!Objects.equals(cr.getStderr(), sr.getStderr())) {
                        testIds.addAll(processSessionIds(false, cr.getStderr()));
                    }
                }
            }
            if (!isDisableUsageStats()) {
                failedTests = testResult.getFailedTests();
                for (CaseResult failedTest : failedTests) {
                    // if this turns out to be too much info we can use getErrorDetails() instead
                    failedTestsMap.put(failedTest.getName(),failedTest.getErrorStackTrace().trim());
                }
            }
        }

        for (TestIDDetails details : testIds) {
            JenkinsJobInformation jobInformation;
            if (onDemandTests.containsKey(details.getJobId())) {
                jobInformation = onDemandTests.get(details.getJobId());
            } else {
                jobInformation = new JenkinsJobInformation(details.getJobId(), "");
                try {
                    jobInformation.populateFromJson(
                        new JSONObject(sauceREST.getJobInfo(details.getJobId()))
                    );
                    onDemandTests.put(jobInformation.getJobId(), jobInformation);
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            Map<String, Object> updates = jobInformation.getChanges();
            //only store passed/name values if they haven't already been set
            if (jobInformation.getStatus() == null) {
                Boolean buildResult = hasTestPassed(testResult, jobInformation);
                if (buildResult != null) {
                    //set the status to passed if the test was successful
                    jobInformation.setStatus(buildResult.booleanValue() ? "passed" : "failed");
                    updates.put("passed", buildResult);
                }
            }
            if (!jobInformation.hasJobName()) {
                jobInformation.setName(details.getJobName());
                updates.put("name", details.getJobName());
            }
            if (!jobInformation.hasBuild()) {
                jobInformation.setBuild(SauceEnvironmentUtil.getSanitizedBuildNumber(build));
                updates.put("build", jobInformation.getBuild());
            }
            if (!Strings.isNullOrEmpty(getJobVisibility())) {
                updates.put("public", getJobVisibility());
            }

            // add the failure message to custom data IF we're sending data, also there may be other custom data we want to preserve
            if (!isDisableUsageStats() && testResult != null && jobInformation.getStatus() == "Failed") {
                try {
                    JSONObject jobDetails = new JSONObject(sauceREST.getJobInfo(details.getJobId()));
                    JSONObject existingCustomData = jobDetails.getJSONObject("custom-data");
                    Map<String, Object> customData = new HashMap<String, Object>();

                    Iterator<String> customDataKeys = existingCustomData.keys();
                    while (customDataKeys.hasNext()) {
                        String customDataKey = customDataKeys.next();
                        customData.put(customDataKey, existingCustomData.getString(customDataKey));
                    }

                    // see if failedTests contains the job name
                    if (failedTestsMap.get(jobInformation.getName())!=null) {
                        customData.put("FAILURE_MESSAGE", failedTestsMap.get(jobInformation.getName()));
                    }
                    updates.put("custom-data", customData);
                } catch (JSONException e) {
                    e.printStackTrace();
                    logger.warning("Could not add failure message: " + e.getMessage());
                }
            }

            if (!updates.isEmpty()) {
                logger.fine("Performing Sauce REST update for " + jobInformation.getJobId());
                sauceREST.updateJobInfo(jobInformation.getJobId(), updates);
            }
        }

        if (onDemandTests.size() > 0) {
            buildAction.setJobs(new LinkedList<JenkinsJobInformation>(onDemandTests.values()));
            try {
                build.save();
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Unable to save build: " + e.getMessage());
            }
        }
    }

    private boolean isDisableUsageStats() {
        PluginImpl plugin = PluginImpl.get();
        if (plugin == null) { return false; }
        return plugin.isDisableUsageStats();
    }

    protected SauceREST getSauceREST(Run build) {
        return SauceOnDemandBuildAction.getSauceBuildAction(build).getCredentials().getSauceREST();
    }

    /**
     * Determines if a Sauce job has passed or failed by attempting to identify a matching test case.
     *
     * A test case is identified as a match if:
     * <ul>
     *     <li>if the job name equals full name of test; or</li>
     *     <li>if job name contains the test name; or</li>
     *     <li>if the full name of the test contains the job name (matching whole words only)</li>
     * </ul>
     *
     * If a match is found, then a boolean representing whether the test passed will be returned.
     *
     * @param testResult Contains the test results for the build.
     * @param job        details of a Sauce job which was run during the build.
     * @return Boolean indicating whether the test was successful.
     */
    @SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
    private Boolean hasTestPassed(TestResult testResult, JenkinsJobInformation job) {

        if (testResult == null) {
            return null;
        }

        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                //if job name matches test class/test name, and pass/fail status is null, then populate the Sauce job with the test result status
                if (job.getName() != null && job.getStatus() == null) {
                    try {
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
                    } catch (Exception e) {
                        //ignore and continue
                        logger.log(Level.WARNING, "Error parsing line, attempting to continue");
                    }

                }
            }
        }
        return null;
    }

    /**
     * Descriptor for the custom publisher.
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
        /**
         *
         * @return the label to be displayed within the Jenkins job configuration.
         */
        @Override
        public String getDisplayName() {
            return "Embed Sauce Labs reports";
        }

        public ListBoxModel doFillJobVisibilityItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("- default -", "");
            items.add("Public", "public");
            items.add("Public Restricted", "public restricted");
            items.add("Private", "private");
            items.add("Team", "team");
            return items;
        }
    }
}
