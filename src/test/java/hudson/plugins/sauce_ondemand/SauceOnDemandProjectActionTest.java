package hudson.plugins.sauce_ondemand;

import com.gargoylesoftware.htmlunit.Page;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.security.AuthorizationStrategy;
import hudson.security.SecurityRealm;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.PresetData;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

public class SauceOnDemandProjectActionTest {

    private SecurityRealm securityRealm;
    private String credentialsId;

    private SecurityRealm getSecurityRealm() {
        if (null == securityRealm) {
            securityRealm = jenkins.createDummySecurityRealm();
        }
        return securityRealm;
    }

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest");
    }

    @Test
    public void testDoGenerateSupportZip() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);
        /* make a build so we can get the build log */
        AbstractBuild b = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
        jenkins.assertBuildStatusSuccess(b);

        Page generateSupportZip;
        try {
            generateSupportZip = jenkins.createWebClient().goTo(
                project.getUrl() + pa.getUrlName() + "/generateSupportZip",
                "application/zip"
            );
        } catch (Exception e) {
            throw e;
        }
        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }

    @Test(expected = com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException.class)
    public void testDoGenerateSupportZip_NoBuildLog() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);

        jenkins.createWebClient().goTo(
            project.getUrl() + pa.getUrlName() + "/generateSupportZip",
            "application/zip"
        );
    }

    @Test
    public void testDoGenerateSupportZip_not_authorized() throws Exception {
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

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


    @PresetData(PresetData.DataSet.ANONYMOUS_READONLY)
    @Test
    public void testDoGenerateSupportZip_authorized() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);
        /* make a build so we can get the build log */
        project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);

        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());

        Page generateSupportZip = jenkins.createWebClient().login("admin", "admin").goTo(
            project.getUrl() + pa.getUrlName() + "/generateSupportZip",
            "application/zip"
        );
        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }
}