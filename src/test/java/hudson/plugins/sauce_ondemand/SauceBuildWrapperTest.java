package hudson.plugins.sauce_ondemand;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.JobSource;
import com.saucelabs.saucerest.api.BuildsEndpoint;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.api.PlatformEndpoint;
import com.saucelabs.saucerest.model.builds.LookupBuildsParameters;
import com.saucelabs.saucerest.model.jobs.Job;
import com.saucelabs.saucerest.model.jobs.UpdateJobParameter;
import com.saucelabs.saucerest.model.platform.Platform;
import com.saucelabs.saucerest.model.platform.SupportedPlatforms;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Maven;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Ross Rowe
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SauceBuildWrapperTest {

  public static final String DEFAULT_TEST_XML = "/hudson/plugins/sauce_ondemand/test-result.xml";

  /** JUnit rule which instantiates a local Jenkins instance with our plugin installed. */
  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private String credentialsId;

  @Before
  public void setUp() throws Exception {
    SystemCredentialsProvider.getInstance().save();
    ToolInstallations.configureDefaultMaven("apache-maven-3.0.1", Maven.MavenInstallation.MAVEN_30);

    this.credentialsId =
        SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest");

    JenkinsSauceREST sauceRest = new JenkinsSauceREST("username", "access key", DataCenter.US_WEST, null);
    PluginImpl plugin = PluginImpl.get();
    assertNotNull(plugin);
    // Reset connection string every run
    plugin.setSauceConnectOptions("");
    plugin.setDisableUsageStats(true);

    JobsEndpoint mockJobsEndpoint = mock(JobsEndpoint.class);
    when(mockJobsEndpoint.updateJob(anyString(), any(UpdateJobParameter.class)))
        .thenReturn(new Job());
    when(mockJobsEndpoint.getJobDetails(anyString())).thenReturn(new Job());

    BuildsEndpoint mockBuildsEndpoint = mock(BuildsEndpoint.class);
    when(mockBuildsEndpoint.lookupBuilds(any(JobSource.class), any(LookupBuildsParameters.class)))
        .thenReturn(new ArrayList<>());

    // create a Mockito spy of the sauceREST instance, to capture REST updates sent by the tests
    JenkinsSauceREST spySauceRest = spy(sauceRest);
    PlatformEndpoint mockPlatform = mock(PlatformEndpoint.class);

    doAnswer(invocationOnMock -> mockJobsEndpoint).when(spySauceRest).getJobsEndpoint();
    doAnswer(invocationOnMock -> mockBuildsEndpoint).when(spySauceRest).getBuildsEndpoint();
    doAnswer(invocation -> "Mocked Rest API").when(spySauceRest).toString();

    List<Platform> appiumPlatforms = new ArrayList<>();
    List<Platform> webdriverPlatforms = new ArrayList<>();
    when(mockPlatform.getSupportedPlatforms("appium"))
        .thenReturn(new SupportedPlatforms(appiumPlatforms));
    when(mockPlatform.getSupportedPlatforms("webdriver"))
        .thenReturn(new SupportedPlatforms(webdriverPlatforms));

    doAnswer(invocationOnMock -> mockPlatform).when(spySauceRest).getPlatformEndpoint();

    // store dummy implementations of Sauce Connect manager

    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            return null;
          }
        };
    storeDummyManager(sauceConnectManager);

    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars envVars = prop.getEnvVars();
    envVars.put("TEST_PORT_VARIABLE_4321", "4321");
    jenkinsRule.getInstance().getGlobalNodeProperties().add(prop);
  }

  @After
  public void tearDown() throws Exception {
    storeDummyManager(null);
  }

  private void storeDummyManager(SauceConnectManager sauceConnectManager) throws Exception {
    HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
    Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectManager");
    field.setAccessible(true);
    field.set(factory, sauceConnectManager);
  }

  /** Verifies that environment variables are resolved for the Sauce Connect options. */
  @Test
  public void resolveVariables() throws Exception {
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            assertEquals(
                "Variable not resolved",
                "-i 1 --region us-west",
                options); // null reverts to default US_WEST
            return null;
          }
        };
    storeDummyManager(sauceConnectManager);
    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    sauceBuildWrapper.setOptions("-i ${BUILD_NUMBER}");

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, null, "resolveVariables");
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  /** Verifies that common options are set when the build is run. */
  @Test
  public void commonOptions() throws Exception {
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            assertEquals(
                "Variables are resolved correctly",
                options,
                "-i 1 --region us-west"); // null reverts to default US
            return null;
          }
        };
    storeDummyManager(sauceConnectManager);
    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    PluginImpl plugin = PluginImpl.get();
    assertNotNull(plugin);
    plugin.setSauceConnectOptions("-i ${BUILD_NUMBER}");
    sauceBuildWrapper.setOptions("");

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, null, "commonOptions");
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  /** Verifies that the options should be common/admin => build => generated */
  @Test
  public void resolvedOptionsOrder() throws Exception {
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            // Match that it starts with tunnel-name, because timestamp
            assertThat(
                "Variables are resolved correctly",
                options,
                CoreMatchers.containsString(
                    "--global --build -i 1 --tunnel-name runFreestyleBuild-resolvedOptionsOrder-"));
            return null;
          }
        };
    storeDummyManager(sauceConnectManager);
    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    PluginImpl plugin = PluginImpl.get();
    assertNotNull(plugin);
    plugin.setSauceConnectOptions("--global");
    sauceBuildWrapper.setOptions("--build -i 1");
    sauceBuildWrapper.setUseGeneratedTunnelIdentifier(true);

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, null, "resolvedOptionsOrder");
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  /** Simulates the handling of a Sauce Connect time out. */
  @Test
  public void sauceConnectTimeOut() throws Exception {
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath)
              throws SauceConnectException {
            throw new SauceConnectDidNotStartException("Sauce Connect failed to start");
          }
        };
    storeDummyManager(sauceConnectManager);
    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, null, "sauceConnectTimeOut");
    jenkinsRule.assertBuildStatus(Result.FAILURE, build);
  }

  /**
   * @throws Exception thrown if an unexpected error occurs
   */
  @Test
  public void multipleBrowsers() throws Exception {

    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    sauceBuildWrapper.setWebDriverBrowsers(
        Arrays.asList("Windows_11MicrosoftEdge115", "Mac_10_15firefox100", ""));

    SauceBuilder sauceBuilder = new SauceBuilderBrowsersExtension();

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder, "multipleBrowsers");
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  /**
   * @throws Exception thrown if an unexpected error occurs
   */
  @Test
  public void newPortIsGeneratedWhenManagingSauceConnect() throws Exception {

    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    sauceBuildWrapper.setEnableSauceConnect(true);
    sauceBuildWrapper.setUseGeneratedTunnelIdentifier(true);

    final JSONObject holder = new JSONObject();
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            holder.element("scProvidedPort", port);
            return null;
          }
        };

    storeDummyManager(sauceConnectManager);
    SauceBuilder sauceBuilder =
        new SauceBuilder() {
          @Override
          public boolean perform(
              AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
              throws InterruptedException, IOException {
            Map<String, String> envVars = build.getEnvironment(listener);
            int port = Integer.parseInt(envVars.get("SELENIUM_PORT"));
            holder.element("port", port);
            return super.perform(build, launcher, listener);
          }
        };

    FreeStyleBuild build =
        runFreestyleBuild(
            sauceBuildWrapper, sauceBuilder, "newPortIsGeneratedWhenManagingSauceConnect");
    jenkinsRule.assertBuildStatusSuccess(build);

    assertThat("greater than 0", holder.getInt("port"), greaterThan(0));
    assertEquals(
        "Port provided to SC is the same as generated",
        holder.getInt("scProvidedPort"),
        holder.getInt("port"));
    assertEquals("Successful Build", build.getResult(), Result.SUCCESS);
  }

  /**
   * @throws Exception thrown if an unexpected error occurs
   */
  @Test
  public void providingPortWithEnvVariableStartsUpOnThatPort() throws Exception {

    String port = "4321";

    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
    sauceBuildWrapper.setEnableSauceConnect(true);
    sauceBuildWrapper.setUseGeneratedTunnelIdentifier(true);
    sauceBuildWrapper.setSeleniumPort("$TEST_PORT_VARIABLE_4321");

    final JSONObject holder = new JSONObject();
    SauceConnectManager sauceConnectManager =
        new SauceConnectManager() {
          @Override
          public Process openConnection(
              String username,
              String apiKey,
              DataCenter dataCenter,
              int port,
              File sauceConnectJar,
              String options,
              PrintStream printStream,
              Boolean verboseLogging,
              String sauceConnectPath) {
            holder.element("scProvidedPort", Integer.toString(port, 10));
            return null;
          }
        };

    storeDummyManager(sauceConnectManager);
    SauceBuilder sauceBuilder =
        new SauceBuilder() {
          @Override
          public boolean perform(
              AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
              throws InterruptedException, IOException {
            Map<String, String> envVars = build.getEnvironment(listener);
            holder.element("env", envVars);
            return super.perform(build, launcher, listener);
          }
        };

    FreeStyleBuild build =
        runFreestyleBuild(
            sauceBuildWrapper, sauceBuilder, "providingPortWithEnvVariableStartsUpOnThatPort");
    jenkinsRule.assertBuildStatusSuccess(build);
    assertEquals(
        "Port Provided as ENV equals port started up on", port, holder.getString("scProvidedPort"));
    Map<String, String> envVars = (Map<String, String>) holder.get("env");
    assertNotNull(envVars);
    assertEquals("Port Provided as ENV equals SELENIUM_PORT", port, envVars.get("SELENIUM_PORT"));
  }

  /**
   * Verifies that common options are set when the build is run.
   *
   * @throws Exception
   */
  @Test
  public void mavenBuild() throws Exception {
    SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);

    MavenModuleSet project = jenkinsRule.createProject(MavenModuleSet.class, "mavenBuildProject");
    project.getBuildWrappersList().add(sauceBuildWrapper);
    project.setScm(
        new SingleFileSCM("pom.xml", SauceOnDemandProjectActionTest.class.getResource("/pom.xml")));
    project.setGoals("clean");

    MavenModuleSetBuild build = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
    assertNotNull(build);
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  /* FIXME - move to setup() */
  private FreeStyleBuild runFreestyleBuild(
      SauceOnDemandBuildWrapper sauceBuildWrapper, TestBuilder builder, String projectName)
      throws Exception {

    if (builder == null) {
      builder = new SauceBuilder();
    }

    FreeStyleProject freeStyleProject =
        jenkinsRule.jenkins.createProject(
            FreeStyleProject.class, "runFreestyleBuild-" + projectName);
    freeStyleProject.getBuildWrappersList().add(sauceBuildWrapper);
    freeStyleProject.getBuildersList().add(builder);

    QueueTaskFuture<FreeStyleBuild> future = freeStyleProject.scheduleBuild2(0);

    FreeStyleBuild build = future.get(1, TimeUnit.MINUTES);

    assertNotNull(build);

    return build;
  }

  /** Dummy builder which is run by the unit tests. */
  @SuppressFBWarnings({"SE_BAD_FIELD_INNER_CLASS", "SE_NO_SERIALVERSIONID"})
  private static class SauceBuilder extends TestBuilder implements Serializable {

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws InterruptedException, IOException {
      SauceCredentials credentials = SauceCredentials.getCredentials(build);
      // assert that mock SC started

      Map<String, String> envVars = build.getEnvironment(listener);

      assertEquals(
          "Environment variable SAUCE_USER_NAME not found",
          credentials.getUsername(),
          envVars.get("SAUCE_USER_NAME"));
      assertEquals(
          "Environment variable SAUCE_USER_NAME not found",
          credentials.getApiKey().getPlainText(),
          envVars.get("SAUCE_API_KEY"));

      File destination = new File(build.getWorkspace().getRemote(), "test.xml");
      FileUtils.copyURLToFile(getClass().getResource(DEFAULT_TEST_XML), destination);
      return true;
    }
  }

  private static final class SauceBuilderBrowsersExtension extends SauceBuilder {
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws InterruptedException, IOException {
      // verify that SAUCE_ environment variables are populated
      Map<String, String> envVars = build.getEnvironment(listener);
      // TODO: this test will fail occasionally
      assertNotNull(
          "Environment variable SAUCE_ONDEMAND_BROWSERS not found",
          envVars.get("SAUCE_ONDEMAND_BROWSERS"));
      return super.perform(build, launcher, listener);
    }
  }
}
