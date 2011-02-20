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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.saucelabs.rest.Credential;
import com.saucelabs.rest.JobFactory;
import hudson.model.*;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;
import hudson.util.IOUtils;
import hudson.util.Secret;

import java.net.URL;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test for {@link hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReportPublisherTest extends BaseTezt {

    public void testReportEmbedding() throws Exception {
        //testReportEmbedding(IOUtils.toString(getClass().getResourceAsStream("test-result.xml")), false);
    }

    public void testReportEmbeddingOld() throws Exception {
        testReportEmbedding(IOUtils.toString(getClass().getResourceAsStream("test-result-old.xml")), true);
    }

    private void testReportEmbedding(String testReport, boolean oldStyle) throws Exception {
        setCredential();
        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(new Tunnel(80, jettyLocalPort, "localhost", "AUTO"));
        p.getBuildWrappersList().add(before);
        JUnitResultArchiver junit = new JUnitResultArchiver(
                "test.xml",
                true,
                new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(
                        Saveable.NOOP,
                        Collections.singletonList(new SauceOnDemandReportPublisher())
                )
        );
        p.getPublishersList().add(junit);
        SauceBuilder sauceBuilder = new SauceBuilder(testReport);
        invokeSeleniumFromBuild(p, sauceBuilder);
        String sessionId = sauceBuilder.sessionId;

        // When I try loading this with javascript enabled, the embedded report causes an error
        WebClient webClient = new WebClient();
        webClient.setJavaScriptEnabled(false);
        HtmlPage page= webClient.getPage(p.getLastBuild(), TeztSimulation.URL);
        String xml = page.asXml();

        if (oldStyle) {
            // should find a link to a separate test reports page
            assertTrue("link to sauce-ondemand-report not found (old style)", xml.contains("<a href=\"sauce-ondemand-report/\">"));
        } else {
            // should find an embedded report on the test result page
            Pattern urlPattern = Pattern.compile(".*<script src=\"(http://saucelabs.com/job-embed/.*\\?auth=.*?)\".*", Pattern.DOTALL);
            Matcher m = urlPattern.matcher(xml);

            assertTrue("test results page doesn't embed report", m.matches());

            String reportURL = m.group(1);
            reportURL = reportURL.replace("http://", "https://"); //redirected
            String embeddedReport = IOUtils.toString(new URL(reportURL).openStream());
            System.out.println("embeddedReport: " + embeddedReport);
            assertTrue("invalid embedded report", embeddedReport.startsWith("document.write") && embeddedReport.contains(sessionId));

            JobFactory factory = new JobFactory(new Credential(PluginImpl.get().getUsername(), Secret.toString(PluginImpl.get().getApiKey())));
            com.saucelabs.rest.Job job = factory.get(sessionId);
            assertEquals(TeztSimulation.JOB_NAME, job.name);
            assertTrue(job.passed);
            assertEquals(1, job.build);
        }
    }


}
