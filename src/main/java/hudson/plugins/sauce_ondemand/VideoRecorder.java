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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Associates Sauce OnDemand session ID to unit tests.
 *
 * @author Kohsuke Kawaguchi
 */
public class VideoRecorder extends TestDataPublisher {
    @DataBoundConstructor
    public VideoRecorder() {
    }

    @Override
    public SauceOnDemandReportFactory getTestData(AbstractBuild<?,?> build, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {
        // start downloading the reports in the background
        DownloadQueue q = PluginImpl.get().download;
        for (SuiteResult sr : testResult.getSuites()) {
            for (CaseResult cr : sr.getCases()) {
                for (String id : SauceOnDemandReportFactory.findSessionIDs(cr)) {
                    q.requestLowPriority(id,build);
                }
            }
        }

        return new SauceOnDemandReportFactory();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
        @Override
        public String getDisplayName() {
            return "Video and server log from Sauce OnDemand";
        }
    }
}
