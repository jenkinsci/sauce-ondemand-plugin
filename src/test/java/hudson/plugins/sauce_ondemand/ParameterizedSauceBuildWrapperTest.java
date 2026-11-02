package hudson.plugins.sauce_ondemand;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import com.saucelabs.saucerest.DataCenter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Launcher;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
@WithJenkins
public class ParameterizedSauceBuildWrapperTest {
  /** JUnit rule which instantiates a local Jenkins instance with our plugin installed. */
  private static JenkinsRule jenkinsRule;

  @BeforeAll
  static void setUpJenkins(JenkinsRule rule) {
    jenkinsRule = rule;
  }

  /** Build Wrapper with all the parameters set right */
  private SauceOnDemandBuildWrapper sauceBuildWrapper;

  public void initParameterizedSauceBuildWrapperTest(
      boolean enableSauceConnect,
      boolean launchSauceConnectOnSlave,
      boolean useGeneratedTunnelIdentifier,
      boolean verboseLogging,
      boolean useLatestVersion,
      boolean forceCleanup,
      String seleniumPort,
      String seleniumHost) {
    sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(null);
    sauceBuildWrapper.setEnableSauceConnect(enableSauceConnect);
    sauceBuildWrapper.setLaunchSauceConnectOnSlave(launchSauceConnectOnSlave);
    sauceBuildWrapper.setUseGeneratedTunnelIdentifier(useGeneratedTunnelIdentifier);
    sauceBuildWrapper.setVerboseLogging(verboseLogging);
    sauceBuildWrapper.setUseLatestVersion(useLatestVersion);
    sauceBuildWrapper.setForceCleanup(forceCleanup);
    sauceBuildWrapper.setSeleniumPort(seleniumPort);
    sauceBuildWrapper.setSeleniumHost(seleniumHost);
  }

  public static Collection sauceOnDemandBuildWrapperValues() {
    ArrayList<Object[]> list = new ArrayList<>();
    for (boolean enableSauceConnect : new boolean[] {true, false}) {
      for (boolean launchSauceConnectOnSlave : new boolean[] {true, false}) {
        for (boolean useGeneratedTunnelIdentifier : new boolean[] {true, false}) {
          for (boolean verboseLogging : new boolean[] {true, false}) {
            for (boolean useLatestVersion : new boolean[] {true, false}) {
              for (boolean forceCleanup :
                  new boolean[] {
                    false
                  }) { // set this to {true, false} for proper testing.  But it will increase test
                // times by about 20m
                for (String seleniumPort : new String[] {"", "4444"}) {
                  for (String seleniumHost : new String[] {"", "localhost"}) {
                    list.add(
                        new Object[] {
                          enableSauceConnect,
                          launchSauceConnectOnSlave,
                          useGeneratedTunnelIdentifier,
                          verboseLogging,
                          useLatestVersion,
                          forceCleanup,
                          seleniumPort,
                          seleniumHost
                        });
                  }
                }
              }
            }
          }
        }
      }
    }
    return list;
  }

  @BeforeEach
  public void setUp() throws Exception {
    String credentialsId =
        SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest");

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
              String sauceConnectPath,
              boolean legacyCLI) {
            return null;
          }
        };

    storeDummyManager(sauceConnectManager);

    JSONObject pluginConfig = new JSONObject();
    pluginConfig.put("username", "fakeuser");
    pluginConfig.put("apiKey", "fakeapi");
    pluginConfig.put("reuseSauceAuth", true);
    pluginConfig.put("sauceConnectDirectory", "");
    pluginConfig.put("sauceConnectOptions", "");
    pluginConfig.put("sauceConnectCLIOptions", "");
    pluginConfig.put("disableStatusColumn", false);
    pluginConfig.put("environmentVariablePrefix", "");
    pluginConfig.put("disableUsageStats", true);
    pluginConfig.put("sauceConnectMaxRetries", "");
    pluginConfig.put("sauceConnectRetryWaitTime", "");

    sauceBuildWrapper.setCredentialId(credentialsId);

    PluginManager manager = jenkinsRule.getPluginManager();
    PluginWrapper plugin = manager.getPlugin("sauce-ondemand");
    assertNotNull(plugin);
    Plugin p2 = plugin.getPlugin();
    assertNotNull(p2);
    p2.configure((StaplerRequest2) null, pluginConfig);
  }

  @AfterEach
  public void tearDown() throws Exception {
    storeDummyManager(null);
  }

  private void storeDummyManager(SauceConnectManager sauceConnectManager) throws Exception {
    HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
    Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectManager");
    field.setAccessible(true);
    field.set(factory, sauceConnectManager);
  }

  /**
   * @throws Exception thrown if an unexpected error occurs
   */
  @MethodSource("sauceOnDemandBuildWrapperValues")
  @ParameterizedTest
  public void portIsProperlyProvidedToSauceConnect(
      boolean enableSauceConnect,
      boolean launchSauceConnectOnSlave,
      boolean useGeneratedTunnelIdentifier,
      boolean verboseLogging,
      boolean useLatestVersion,
      boolean forceCleanup,
      String seleniumPort,
      String seleniumHost)
      throws Exception {
    initParameterizedSauceBuildWrapperTest(
        enableSauceConnect,
        launchSauceConnectOnSlave,
        useGeneratedTunnelIdentifier,
        verboseLogging,
        useLatestVersion,
        forceCleanup,
        seleniumPort,
        seleniumHost);
    if (!sauceBuildWrapper.isEnableSauceConnect()) {
      return;
    } /* Don't test sauce connect functionality if its not enabled */

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
              String sauceConnectPath,
              boolean legacyCLI) {
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

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder);
    assertThat("greater than 0", holder.getInt("port"), greaterThan(0));
    assertEquals(
        holder.getInt("scProvidedPort"),
        holder.getInt("port"),
        "Port provided to SC is the same as generated");
    jenkinsRule.assertBuildStatusSuccess(build);
  }

  @MethodSource("sauceOnDemandBuildWrapperValues")
  @ParameterizedTest
  public void confirmEnvVariablesAreAlwaysSet(
      boolean enableSauceConnect,
      boolean launchSauceConnectOnSlave,
      boolean useGeneratedTunnelIdentifier,
      boolean verboseLogging,
      boolean useLatestVersion,
      boolean forceCleanup,
      String seleniumPort,
      String seleniumHost)
      throws Exception {
    initParameterizedSauceBuildWrapperTest(
        enableSauceConnect,
        launchSauceConnectOnSlave,
        useGeneratedTunnelIdentifier,
        verboseLogging,
        useLatestVersion,
        forceCleanup,
        seleniumPort,
        seleniumHost);
    final JSONObject holder = new JSONObject();
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

    FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder);
    jenkinsRule.assertBuildStatusSuccess(build);

    Map<String, String> envVars = (Map<String, String>) holder.get("env");
    assertNotNull(envVars);
    assertEquals(
        "fakeuser",
        envVars.get("SAUCE_USER_NAME"),
        "legacy SAUCE_USER_NAME is set to API username");
    assertEquals(
        "fakeuser", envVars.get("SAUCE_USERNAME"), "proper SAUCE_USERNAME is set to API username");
    assertEquals(
        "fakekey", envVars.get("SAUCE_API_KEY"), "legacy SAUCE_API_KEY is set to API username");
    assertEquals(
        "fakekey",
        envVars.get("SAUCE_ACCESS_KEY"),
        "proper SAUCE_ACCESS_KEY is set to API username");
    assertThat(
        "SELENIUM_HOST equals something", envVars.get("SELENIUM_HOST"), not(emptyOrNullString()));
    assertThat(
        "SELENIUM_PORT equals something", envVars.get("SELENIUM_PORT"), not(emptyOrNullString()));
    assertThat(
        "JENKINS_BUILD_NUMBER equals something",
        envVars.get("JENKINS_BUILD_NUMBER"),
        not(emptyOrNullString()));

    if (!"".equals(sauceBuildWrapper.getNativeAppPackage())) {
      assertNull(
          envVars.get("SAUCE_NATIVE_APP"),
          "SAUCE_NATIVE_APP is not set when native package is not set");
    } else {
      assertThat(
          "SAUCE_NATIVE_APP is set when native package is set",
          envVars.get("SAUCE_NATIVE_APP"),
          not(emptyOrNullString()));
    }
    if (sauceBuildWrapper.isEnableSauceConnect()
        && sauceBuildWrapper.isUseGeneratedTunnelIdentifier()) {
      assertThat(
          "TUNNEL_NAME is set when we are managing it",
          envVars.get("TUNNEL_NAME"),
          not(emptyOrNullString()));

    } else {
      assertNull(
          envVars.get("SAUCE_NATIVE_APP"), "TUNNEL_NAME is not set when we are not managing it");
    }
    if (sauceBuildWrapper.isUseChromeForAndroid()) {
      assertNull(
          envVars.get("SAUCE_USE_CHROME"),
          "SAUCE_USE_CHROME is not set when use chrome is not set");
    } else {
      assertThat(
          "SAUCE_USE_CHROME is set when use chrome is set",
          envVars.get("SAUCE_USE_CHROME"),
          not(emptyOrNullString()));
    }
    /*
    SELENIUM_PLATFORM, SELENIUM_BROWSER, SELENIUM_VERSION, SELENIUM_DRIVER, SELENIUM_DEVICE, SELENIUM_DEVICE_TYPE, SELENIUM_DEVICE_ORIENTATION
    SAUCE_ONDEMAND_BROWSERS
    */
  }

  private FreeStyleBuild runFreestyleBuild(
      SauceOnDemandBuildWrapper sauceBuildWrapper, TestBuilder builder) throws Exception {
    FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
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
      Thread.sleep(1);
      return true;
    }
  }
}