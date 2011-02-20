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

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.JobFactory;
import com.saucelabs.rest.UpdateJob;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public SauceOnDemandReportFactory getTestData(AbstractBuild<?,?> build, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {
        JobFactory factory = new JobFactory(new Credential(PluginImpl.get().getUsername(), Secret.toString(PluginImpl.get().getApiKey())));

        buildListener.getLogger().println("Scanning for Sauce OnDemand test data...");
        boolean hasResult = false;
        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                String jobName = cr.getFullName();

                List<String> sessionIDs = SauceOnDemandReportFactory.findSessionIDs(jobName, cr.getStdout(), cr.getStderr());
                for (String id : sessionIDs) {
                    hasResult = true;
                    try {
                        factory.update(id, new UpdateJob(cr.getFullName(), false, Collections.<String>emptyList(), build.getNumber(), cr.isPassed(), Collections.<String, Object>emptyMap()));
                    } catch (IOException e) {
                        e.printStackTrace(buildListener.error("Error while updating job " + id));
                    }
                }
                if (sessionIDs.isEmpty()) {
                    // check for old-style logs
                    sessionIDs = SauceOnDemandReportFactory.findSessionIDs(null, cr.getStdout(), cr.getStderr());
                    if (!sessionIDs.isEmpty()) {
                        hasResult = true;
                    }
                }
            }
        }

        if (!hasResult) {
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
