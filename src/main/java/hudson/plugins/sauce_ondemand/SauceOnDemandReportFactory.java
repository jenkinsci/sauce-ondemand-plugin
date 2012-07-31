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

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contribute s{@link SauceOnDemandReport} to {@link CaseResult}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReportFactory extends Data {

    private static final Logger logger = Logger.getLogger(SauceOnDemandReportFactory.class.getName());

    public static final SauceOnDemandReportFactory INSTANCE = new SauceOnDemandReportFactory();

    /**
     * Makes this a singleton -- since it's stateless, there's no need to keep one around for every build.
     *
     * @return
     */
    public Object readResolve() {
        return INSTANCE;
    }

    @Override
    public List<SauceOnDemandReport> getTestAction(TestObject testObject) {

        if (testObject instanceof CaseResult) {
            logger.log(Level.INFO, "Attempting to find Sauce SessionID for test object");
            CaseResult cr = (CaseResult) testObject;
            String jobName = cr.getFullName();
            List<String[]> ids = findSessionIDs(jobName, cr.getStdout(), cr.getStderr());
            boolean matchingJobNames = true;
            if (ids.isEmpty()) {
                // fall back to old-style log parsing (when no job-name is present in output)
                logger.log(Level.INFO, "Parsing stdout with no job name");
                ids = findSessionIDs(null, cr.getStdout(), cr.getStderr());
                if (ids.isEmpty()) {
                    ids = SauceOnDemandReportFactory.findSessionIDs(null, cr.getStdout(), cr.getStderr());
                }
                matchingJobNames = false;
            }


            if (ids.isEmpty()) {
                logger.log(Level.WARNING, "Unable to find Sauce SessionID for test object");
            } else {
                return Collections.singletonList(new SauceOnDemandReport(cr, ids, matchingJobNames));
            }
        } else {
            logger.log(Level.INFO, "Test Object not a CaseResult, unable to parse output");
        }
        return Collections.emptyList();
    }

    /**
     * Returns all sessions matching a given jobName in the provided logs.
     * If no session is found for the jobName, return all session that do not provide job-name (old format)
     */
    static List<String[]> findSessionIDs(String jobName, String... output) {
        List<String[]> sessions = new ArrayList<String[]>();
        Pattern p = jobName != null ? SESSION_ID_PATTERN : OLD_SESSION_ID_PATTERN;
        for (String text : output) {
            if (text == null) continue;
            Matcher m = p.matcher(text);
            while (m.find()) {
                String sessionId = m.group(1);
                String job = "";
                if (m.groupCount() == 2 && (jobName == null || jobName.equals(m.group(2)))) {
                    job = m.group(2);
                }
                sessions.add(new String[]{sessionId, job});
            }
        }
        return sessions;
    }

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+) job-name=(.*)");
    private static final Pattern OLD_SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)");


}
