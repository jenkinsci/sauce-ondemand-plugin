package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.JobSource;
import com.saucelabs.saucerest.api.BuildsEndpoint;
import com.saucelabs.saucerest.model.builds.LookupBuildsParameters;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WithJenkins
class SauceOnDemandProjectActionTest {

    private final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
    private final BuildsEndpoint mockBuildsEndpoint = mock(BuildsEndpoint.class);
    private String credentialsId;

    private static JenkinsRule jenkins;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() throws Exception {
        credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest");
        PluginImpl plugin = PluginImpl.get();
        assertNotNull(plugin);
        // Reset connection string every run
        plugin.setSauceConnectOptions("");
        plugin.setDisableUsageStats(true);
    }

    @Test
    void testDoGenerateSupportZip() throws Exception {
        when(mockBuildsEndpoint.lookupBuilds(any(JobSource.class), any(LookupBuildsParameters.class)))
            .thenReturn(Collections.emptyList());
        when(mockSauceREST.getBuildsEndpoint()).thenReturn(mockBuildsEndpoint);

        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildWrappersList().add(sauceBuildWrapper);

        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        /* make a build so we can get the build log */
        AbstractBuild build = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
        SauceOnDemandBuildAction buildAction =
            new SauceOnDemandBuildAction(build, sauceBuildWrapper.getCredentialId()) {
                @Override
                protected JenkinsSauceREST getSauceREST() {
                    return mockSauceREST;
                }
            };
        build.addAction(buildAction);

        jenkins.assertBuildStatusSuccess(build);

        Page generateSupportZip =
            jenkins
                .createWebClient()
                .goTo(project.getUrl() + pa.getUrlName() + "/generateSupportZip", "application/zip");

        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }

    @Test
    void testDoGenerateSupportZip_NoBuildLog() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);
        assertThrows(FailingHttpStatusCodeException.class, () ->
            jenkins
                .createWebClient()
                .goTo(project.getUrl() + pa.getUrlName() + "/generateSupportZip", "application/zip"));
    }

    @Test
    void testDoGenerateSupportZip_not_authorized() throws Exception {
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);

        /* make a build so we can get the build log */
        AbstractBuild b = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
        jenkins.assertBuildStatusSuccess(b);

        Page generateSupportZip =
            jenkins
                .createWebClient()
                .goTo(project.getUrl() + pa.getUrlName() + "/generateSupportZip", "application/zip");
        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }

    @Test
    void testDoGenerateSupportZip_authorized() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setEnableSauceConnect(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        SauceOnDemandProjectAction pa = new SauceOnDemandProjectAction(project);
        project.getBuildWrappersList().add(sauceBuildWrapper);
        /* make a build so we can get the build log */
        project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);

        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());

        Page generateSupportZip =
            jenkins
                .createWebClient()
                .login("admin", "admin")
                .goTo(project.getUrl() + pa.getUrlName() + "/generateSupportZip", "application/zip");
        assertNotNull(generateSupportZip);
        jenkins.assertGoodStatus(generateSupportZip);
    }
}
