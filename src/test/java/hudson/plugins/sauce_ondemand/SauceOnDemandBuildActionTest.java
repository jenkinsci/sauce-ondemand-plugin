package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, bw.getCredentialId()) {
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
}