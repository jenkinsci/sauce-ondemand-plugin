package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SauceOnDemandBuildActionTest {
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void doJobReportTest() throws Exception {
        final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
        when(mockSauceREST.getBuildFullJobs(anyString())).thenReturn(
            IOUtils.toString(SauceOnDemandProjectActionTest.class.getResourceAsStream("/build_jobs.json"), "UTF-8")
        );
        when(mockSauceREST.getTunnels()).thenReturn("[]");

        FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
        TestSauceOnDemandBuildWrapper bw = new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest")
        );
        bw.setEnableSauceConnect(false);
        freeStyleProject.getBuildWrappersList().add(bw);
        Build build = freeStyleProject.scheduleBuild2(0).get();
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, bw.getCredentialId()) {
            @Override
            protected JenkinsSauceREST getSauceREST() {
                //mockSauceREST.setServer("http://localhost:61325"); not the failure
                return mockSauceREST;
            }
        };
        build.addAction(buildAction);

        JenkinsRule.WebClient webClient = jenkins.createWebClient();
        webClient.setJavaScriptEnabled(false);
        HtmlPage page = webClient.getPage(build, "sauce-ondemand-report/jobReport?jobId=1234");
        jenkins.assertGoodStatus(page);

        DomElement scriptTag = getEmbedTag(page.getElementsByTagName("iframe"));
        assertThat(new URL(scriptTag.getAttribute("src")).getPath(), endsWith("/job-embed/1234"));
        assertThat(new URL(scriptTag.getAttribute("src")).getQuery(), containsString("auth="));
        verifyNoMoreInteractions(mockSauceREST);
    }

    @Test
    public void testGetSauceBuildAction () throws Exception {
        FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
        TestSauceOnDemandBuildWrapper bw = new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest")
        );
        bw.setEnableSauceConnect(false);
        freeStyleProject.getBuildWrappersList().add(bw);
        Build build = freeStyleProject.scheduleBuild2(0).get();
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, bw.getCredentialId());
        build.addAction(buildAction);
        SauceOnDemandBuildAction  sauceBuildAction= SauceOnDemandBuildAction.getSauceBuildAction(build);
        assertEquals("fakeuser", sauceBuildAction.getCredentials().getUsername());
    }

    @Test
    public void testGetSauceBuildActionMavenBuild() throws Exception {
        MavenModuleSet project = jenkins.createProject(MavenModuleSet.class, "testGetSauceBuildActionMavenBuild");
        TestSauceOnDemandBuildWrapper bw = new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest")
        );
        project.getBuildWrappersList().add(bw);
        project.setScm(new SingleFileSCM("pom.xml",getClass().getResource("/pom.xml")));
        project.setGoals("clean");
        MavenModuleSetBuild build =  project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
        SauceOnDemandBuildAction buildAction = new SauceOnDemandBuildAction(build, bw.getCredentialId());
        build.addAction(buildAction);
        final MavenBuild mavenBuildMock = mock(MavenBuild.class);
        when(mavenBuildMock.getParentBuild()).thenReturn(build);
        SauceOnDemandBuildAction sauceBuildAction = SauceOnDemandBuildAction.getSauceBuildAction(mavenBuildMock);
        assertEquals("fakeuser", sauceBuildAction.getCredentials().getUsername());

    }


    private DomElement getEmbedTag(DomNodeList<DomElement> scripts) {
        for(DomElement htmlElement : scripts)
        {
            if (htmlElement.getAttribute("src").contains("job-embed")) {
                return htmlElement;
            }
        }
        return null;
    }
}