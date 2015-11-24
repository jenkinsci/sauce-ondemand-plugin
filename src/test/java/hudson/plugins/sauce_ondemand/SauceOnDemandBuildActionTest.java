package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.saucelabs.saucerest.SauceREST;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by gavinmogan on 2015-11-22.
 */
public class SauceOnDemandBuildActionTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void doJobReportTest() throws Exception {
        final JenkinsSauceREST mockSauceREST = new MockSauceREST() {
            @Override
            public String getBuildJobs(String build, boolean full) {
                try {
                    return IOUtils.toString(getClass().getResourceAsStream("build_jobs.json"), "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                    return "";
                }
            }
        };

        FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
        Build build = freeStyleProject.scheduleBuild2(0).get();
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, null, "fake", "fake") {
            @Override
            protected JenkinsSauceREST getSauceREST() {
                return mockSauceREST;
            }
        };
        build.addAction(buildAction);

        JenkinsRule.WebClient webClient = jenkins.createWebClient();
        webClient.setJavaScriptEnabled(false);
        HtmlPage page = webClient.getPage(build, "sauce-ondemand-report/jobReport?jobId=1234");
        jenkins.assertGoodStatus(page);
        HtmlElement scriptTag = getEmbedTag(page.getElementsByTagName("script"));
        assertThat(new URL(scriptTag.getAttribute("src")).getQuery(), containsString("/job-embed/1234.js?auth="));
    }

    private HtmlElement getEmbedTag(DomNodeList<HtmlElement> scripts) {
        for(HtmlElement htmlElement : scripts)
        {
            if (htmlElement.getAttribute("src").contains("job-embed")) {
                return htmlElement;
            }
        }
        return null;
    }
}