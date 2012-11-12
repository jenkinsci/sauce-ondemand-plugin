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

import com.saucelabs.saucerest.SauceREST;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.util.Secret;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Associates Sauce OnDemand session ID to unit tests.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReportPublisher extends TestDataPublisher {
    @DataBoundConstructor
    public SauceOnDemandReportPublisher() {
    }

    @Override
    public SauceOnDemandReportFactory getTestData(AbstractBuild<?, ?> build, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {

        buildListener.getLogger().println("Scanning for Sauce OnDemand test data...");
        List<String> lines = IOUtils.readLines(build.getLogReader());
        String[] array = lines.toArray(new String[lines.size()]);
        List<String[]> sessionIDs = new ArrayList<String[]>();
        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                sessionIDs.addAll(SauceOnDemandReportFactory.findSessionIDs(cr, cr.getStdout(), cr.getStderr()));
            }
        }
        if (sessionIDs.isEmpty()) {
            sessionIDs.addAll(SauceOnDemandReportFactory.findSessionIDs(null, array));
        }
        SauceOnDemandBuildAction buildAction = build.getAction(SauceOnDemandBuildAction.class);
        if (buildAction == null) {
            buildListener.getLogger().println("Unable to retrieve the Sauce Build Action, attempting to continue");
        } else {
            SauceREST sauceREST = new SauceREST(buildAction.getUsername(), buildAction.getAccessKey());
            for (String[] id : sessionIDs) {
                try {
                    String json = sauceREST.getJobInfo(id[0]);
                    JSONObject jsonObject = new JSONObject(json);
                    Map<String, Object> updates = new HashMap<String, Object>();
                    //only store passed/name values if they haven't already been set
                    if (jsonObject.get("passed").equals(JSONObject.NULL) && id.length == 3) {
                        updates.put("passed", id[2]);
                    }
                    if (jsonObject.get("name").equals(JSONObject.NULL)) {
                        updates.put("name", id[1]);
                    }
                    updates.put("build", SauceOnDemandBuildWrapper.sanitiseBuildNumber(build.toString()));
                    sauceREST.updateJobInfo(id[0], updates);
                } catch (IOException e) {
                    e.printStackTrace(buildListener.error("Error while updating job " + id));
                } catch (JSONException e) {
                    e.printStackTrace(buildListener.error("Error while updating job " + id));
                }
            }
        }
        buildListener.getLogger().println("Finished scanning for Sauce OnDemand test data...");
        if (sessionIDs.isEmpty()) {
            buildListener.getLogger().println("The Sauce OnDemand plugin is configured, but no session IDs were found in the test output.");
            return null;
        } else {
            return SauceOnDemandReportFactory.INSTANCE;
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
        @Override
        public String getDisplayName() {
            return "Embed Sauce OnDemand reports";
        }
    }
}
