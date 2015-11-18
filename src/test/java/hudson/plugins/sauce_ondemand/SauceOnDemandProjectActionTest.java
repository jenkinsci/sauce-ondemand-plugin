package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.Page;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * Created by gavinmogan on 2015-11-17.
 */
public class SauceOnDemandProjectActionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testDoGenerateSupportZip() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(new Credentials("fake","fake"));

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);
        /* make a build so we can get the build log */
        project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);

        Page generateSupportZip = jenkins.createWebClient().goTo(
            project.getUrl() + pa.getUrlName() + "/generateSupportZip",
            "application/zip"
        );
        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }

    @Test(expected = com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException.class)
    public void testDoGenerateSupportZip_NoBuildLog() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(new Credentials("fake","fake"));

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);

        Page generateSupportZip = jenkins.createWebClient().goTo(
            project.getUrl() + pa.getUrlName() + "/generateSupportZip",
            "application/zip"
        );
    }
}