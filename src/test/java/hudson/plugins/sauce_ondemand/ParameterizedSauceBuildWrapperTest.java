package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.hudson.HudsonSauceManagerFactory;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * Created by gavinmogan on 10/14/15.
 */
@RunWith(Parameterized.class)
public class ParameterizedSauceBuildWrapperTest {
    /**
     * JUnit rule which instantiates a local Jenkins instance with our plugin installed.
     */
    @Rule
    public transient JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Dummy credentials to be used by the test.
     */
    private Credentials sauceCredentials = new Credentials("username", "access key");

    /**
     * Build Wrapper with all the parameters set right
     */
    private SauceOnDemandBuildWrapper sauceBuildWrapper;

    public ParameterizedSauceBuildWrapperTest(
            boolean enableSauceConnect,
            boolean launchSauceConnectOnSlave,
            boolean useGeneratedTunnelIdentifier,
            boolean useLatestVersion,
            String seleniumPort,
            String seleniumHost
    ) {
        super();
        sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(sauceCredentials);
        sauceBuildWrapper.setEnableSauceConnect(enableSauceConnect);
        sauceBuildWrapper.setLaunchSauceConnectOnSlave(launchSauceConnectOnSlavex);
        sauceBuildWrapper.setUseGeneratedTunnelIdentifier(useGeneratedTunnelIdentifier);
        sauceBuildWrapper.setUseLatestVersion(useLatestVersion);
        sauceBuildWrapper.setSeleniumPort(seleniumPort);
        sauceBuildWrapper.setSeleniumHost(seleniumHost);
    }

    @Parameterized.Parameters
    public static Collection SauceOnDemandBuildWrapperValues() {
        ArrayList<Object[]> list = new ArrayList<Object[]>();
        for(boolean enableSauceConnect : new boolean[] {true}) {
            for(boolean launchSauceConnectOnSlave : new boolean[] { true, false }) {
                for (boolean useGeneratedTunnelIdentifier : new boolean[]{true, false}) {
                    for (boolean verboseLogging : new boolean[]{true, false}) {
                        for (boolean useLatestVersion : new boolean[]{true, false}) {
                            for (String seleniumPort : new String[]{"", "4444"}) {
                                for (String seleniumHost : new String[]{"", "localhost"}) {
                                    list.add(new Object[]{
                                            enableSauceConnect,
                                            launchSauceConnectOnSlave,
                                            useGeneratedTunnelIdentifier,
                                            useLatestVersion,
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
        return list;
    }

    @Before
    public void setUp() throws Exception {
    }

    private void storeDummyManager(SauceConnectFourManager sauceConnectFourManager) throws Exception {
        HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
        Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectFourManager");
        field.setAccessible(true);
        field.set(factory, sauceConnectFourManager);
    }

    /**
     * @throws Exception thrown if an unexpected error occurs
     */
    @Test
    public void portIsProperlyProvidedToSauceConnect() throws Exception {
        final JSONObject holder = new JSONObject();

        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options, PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                holder.element("scProvidedPort", port);
                return null;
            }
        };

        storeDummyManager(sauceConnectFourManager);
        SauceBuilder sauceBuilder = new SauceBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                Map<String, String> envVars = build.getEnvironment(listener);
                int port = Integer.parseInt(envVars.get("SELENIUM_PORT"));
                holder.element("port", port);
                return super.perform(build, launcher, listener);
            }
        };

        FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder);
        assertThat("greater than 0", holder.getInt("port"), greaterThan(0));
        assertEquals("Port provided to SC is the same as generated", holder.getInt("scProvidedPort"), holder.getInt("port"));
        assertEquals("Successful Build", build.getResult(), Result.SUCCESS);
    }

    private FreeStyleBuild runFreestyleBuild(SauceOnDemandBuildWrapper sauceBuildWrapper, TestBuilder builder) throws Exception {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        freeStyleProject.getBuildWrappersList().add(sauceBuildWrapper);
        freeStyleProject.getBuildersList().add(builder);

        QueueTaskFuture<FreeStyleBuild> future = freeStyleProject.scheduleBuild2(0);

        FreeStyleBuild build = future.get(1, TimeUnit.MINUTES);

        assertNotNull(build);
        return build;
    }

    /**
     * Dummy builder which is run by the unit tests.
     */
    private class SauceBuilder extends TestBuilder implements Serializable {

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            Thread.sleep(1000);
            return true;
        }
    }

}
