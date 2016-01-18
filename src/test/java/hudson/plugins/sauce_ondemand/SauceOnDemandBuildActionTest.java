package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.saucelabs.ci.JobInformation;
import hudson.model.Build;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SauceOnDemandBuildActionTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void doJobReportTest() throws Exception {
        final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
        when(mockSauceREST.getBuildFullJobs(anyString())).thenReturn(
            IOUtils.toString(getClass().getResourceAsStream("/build_jobs.json"), "UTF-8")
        );
        when(mockSauceREST.getTunnels()).thenReturn("[]");

        FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
        TestSauceOnDemandBuildWrapper bw = new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest")
        );
        bw.setEnableSauceConnect(false);
        freeStyleProject.getBuildWrappersList().add(bw);
        Build build = freeStyleProject.scheduleBuild2(0).get();
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, null) {
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

        assertThat(new URL(scriptTag.getAttribute("src")).getPath(), endsWith("/job-embed/1234.js"));
        assertThat(new URL(scriptTag.getAttribute("src")).getQuery(), containsString("auth="));

        verifyNoMoreInteractions(mockSauceREST);
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

    private SauceOnDemandBuildAction createFakeAction() throws Exception {
        SauceOnDemandBuildAction sauceOnDemandBuildAction;
        FreeStyleProject project = jenkins.createFreeStyleProject();
        String credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest");
        TestSauceOnDemandBuildWrapper bw = new TestSauceOnDemandBuildWrapper(credentialsId);
        bw.setEnableSauceConnect(false);
        project.getBuildWrappersList().add(bw);
        FreeStyleBuild build = project.scheduleBuild2(0).get(1, TimeUnit.SECONDS);

        sauceOnDemandBuildAction = new SauceOnDemandBuildAction(build, null) {
            @Override
            protected JenkinsSauceREST getSauceREST() {
                return new JenkinsSauceREST("fakeuser","") {
                    @Override
                    public String getJobInfo(String jobId) {
                        try {
                            return IOUtils.toString(getClass().getResourceAsStream("/job_info.json"), "UTF-8");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }
        };
        return sauceOnDemandBuildAction;
    }

    @Test
    public void testProcessSessionIds_none() throws Exception {
        SauceOnDemandBuildAction sauceOnDemandBuildAction;

        sauceOnDemandBuildAction = createFakeAction();
        sauceOnDemandBuildAction.processSessionIds(null, new String[]{});
        assertFalse(sauceOnDemandBuildAction.hasSauceOnDemandResults());
    }

    @Test
    public void testProcessSessionIds_one() throws Exception{
        SauceOnDemandBuildAction sauceOnDemandBuildAction;
        List<JobInformation> jobs;

        sauceOnDemandBuildAction = createFakeAction();
        sauceOnDemandBuildAction.processSessionIds(null, new String[]{
            "SauceOnDemandSessionID=abc123 job-name=gavin"
        });
        assertTrue(sauceOnDemandBuildAction.hasSauceOnDemandResults());
        jobs = sauceOnDemandBuildAction.getJobs();
        assertEquals(1, jobs.size());
        assertEquals("abc123", jobs.get(0).getJobId());
    }

    @Test
    public void testProcessSessionIds_two() throws Exception {
        SauceOnDemandBuildAction sauceOnDemandBuildAction;
        List<JobInformation> jobs;

        sauceOnDemandBuildAction = createFakeAction();
        sauceOnDemandBuildAction.processSessionIds(null, new String[] {
            "SauceOnDemandSessionID=abc123 job-name=gavin\n[firefox 32 OS X 10.10 #1-5] SauceOnDemandSessionID=941b498c5ad544dba92fe73fabfa9eb6 job-name=Insert Job Name Here"
        });
        assertTrue(sauceOnDemandBuildAction.hasSauceOnDemandResults());
        jobs = sauceOnDemandBuildAction.getJobs();
        assertEquals(2, jobs.size());
        assertEquals("abc123", jobs.get(0).getJobId());
        assertEquals("941b498c5ad544dba92fe73fabfa9eb6", jobs.get(1).getJobId());

    }
}