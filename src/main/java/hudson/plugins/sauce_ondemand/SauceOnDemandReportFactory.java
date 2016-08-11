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
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contributes {@link SauceOnDemandReport} to {@link CaseResult}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReportFactory extends Data {

    private static final Logger logger = Logger.getLogger(SauceOnDemandReportFactory.class.getName());

    public static final SauceOnDemandReportFactory INSTANCE = new SauceOnDemandReportFactory();

    private static final String JOB_NAME_PATTERN = "\\b({0})\\b";

    /**
     * Makes this a singleton -- since it's stateless, there's no need to keep one around for every build.
     *
     * @return Singleton Report Factory Instance
     */
    public Object readResolve() {
        return INSTANCE;
    }

    @Override
    public List<SauceOnDemandReport> getTestAction(TestObject testObject) {

        if (testObject instanceof CaseResult) {
            logger.log(Level.FINE, "Attempting to find Sauce SessionID for test object");
            CaseResult cr = (CaseResult) testObject;
            String jobName = cr.getFullName();
            List<String[]> ids = new ArrayList<String[]>();

            AbstractBuild<?, ?> build = cr.getOwner();
            SauceOnDemandBuildAction buildAction = SauceOnDemandBuildAction.getSauceBuildAction(build);
            if (buildAction != null) {
                List<JobInformation> jobs = buildAction.getJobs();
                for (JobInformation job : jobs) {
                    //if job name matches test class/test name, then add id
                    if (job.getName() != null) {
                        Pattern jobNamePattern = Pattern.compile(MessageFormat.format(JOB_NAME_PATTERN, job.getName()));
                        Matcher matcher = jobNamePattern.matcher(cr.getFullName());
                        if (job.getName().equals(cr.getFullName()) //if job name equals full name of test
                                || job.getName().contains(cr.getDisplayName()) //or if job name contains the test name
                                || matcher.find()) { //or if the full name of the test contains the job name (matching whole words only)
                            //then we have a match
                            ids.add(new String[]{job.getJobId(), job.getHmac()});
                        }
                    }
                }
            }

            if (ids.isEmpty()) {
                ids.addAll(findSessionIDs(cr, jobName, cr.getStdout(), cr.getStderr()));
            }

            if (ids.isEmpty()) {
                logger.log(Level.WARNING, "Unable to find Sauce SessionID for test object");
            }

            if (!ids.isEmpty()) {
                return Collections.singletonList(new SauceOnDemandReport(cr, ids));
            }

        } else {
            logger.log(Level.FINE, "Test Object not a CaseResult, unable to parse output: " + testObject.toString());
        }
        return Collections.emptyList();
    }

    /**
     * Returns all sessions matching a given jobName in the provided logs.
     * If no session is found for the jobName, return all session that do not provide job-name (old format)
     */
    static List<String[]> findSessionIDs(CaseResult caseResult, String... output) {

        logger.log(Level.FINE, caseResult == null ? "Parsing Sauce Session ids in stdout" : "Parsing Sauce Session ids in test results");
        List<String[]> sessions = new ArrayList<String[]>();
        for (String text : output) {
            if (text == null) continue;
            Matcher m = SauceOnDemandBuildAction.SESSION_ID_PATTERN.matcher(text);
            while (m.find()) {
                String sessionId = m.group(1);
                String job = "";
                if (m.groupCount() == 2) {
                    job = m.group(2);
                }
                if (caseResult == null) {
                    sessions.add(new String[]{sessionId, job});
                } else {
                    sessions.add(new String[]{sessionId, job, String.valueOf(caseResult.isPassed())});
                }
            }
        }
        return sessions;
    }
}
