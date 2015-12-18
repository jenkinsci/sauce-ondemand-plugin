package hudson.plugins.sauce_ondemand;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.hudson.HudsonSauceManagerFactory;
import com.saucelabs.saucerest.SauceREST;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sauce_ondemand.credentials.impl.SauceCredentialsImpl;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.lang.reflect.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.hamcrest.Matchers.*;

/**
 * @author Ross Rowe
 */
public class SauceBuildWrapperTest {

    /**
     * JUnit rule which instantiates a local Jenkins instance with our plugin installed.
     */
    @Rule
    public transient JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Mockito spy of the Sauce REST instance, used to capture REST requests without sending them to Sauce Labs.
     */
    private JenkinsSauceREST spySauceRest;

    /**
     * Map of data sent over REST, keyed on Sauce Job id.
     */
    private HashMap<String, Map> restUpdates = new HashMap<String, Map>();

    private static final String DEFAULT_SESSION_ID = "0123345abc";

    public static final String DEFAULT_TEST_XML = "/hudson/plugins/sauce_ondemand/test-result.xml";

    private String currentSessionId = DEFAULT_SESSION_ID;

    private String currentTestResultFile = DEFAULT_TEST_XML;
    private String credentialsId;

    @Before
    public void setUp() throws Exception {
        SystemCredentialsProvider.getInstance().save();
        this.credentialsId = SauceCredentialsImpl.migrateToCredentials("fakeuser", "fakekey", "unittest");

        JenkinsSauceREST sauceRest = new JenkinsSauceREST("username", "access key");
        // Reset connection string every run
        PluginImpl.get().setSauceConnectOptions("");

        //create a Mockito spy of the sauceREST instance, to capture REST updates sent by the tests
        spySauceRest = spy(sauceRest);
        restUpdates = new HashMap<String, Map>();
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                restUpdates.put((String) args[0], (Map) args[1]);
                return null;
            }
        }).when(spySauceRest).updateJobInfo(anyString(), any(HashMap.class));
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "{}";
            }
        }).when(spySauceRest).getJobInfo(anyString());
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "{}";
            }
        }).when(spySauceRest).retrieveResults(anyString());

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "{}";
            }
        }).when(spySauceRest).retrieveResults(any(URL.class));

        //store dummy implementations of Sauce Connect manager

        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                return null;
            }
        };
        storeDummyManager(sauceConnectFourManager);

        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("TEST_PORT_VARIABLE_4321", "4321");
        this.jenkinsRule.getInstance().getGlobalNodeProperties().add(prop);
    }

    private void storeDummyManager(SauceConnectFourManager sauceConnectFourManager) throws Exception {
	    HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
		Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectFourManager");
        field.setAccessible(true);
        field.set(factory, sauceConnectFourManager);

    }

    /**
     * Verifies that environment variables are resolved for the Sauce Connect options.
     *
     * @throws Exception
     */
    @Test
    public void resolveVariables() throws Exception {
        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                assertTrue("Variable not resolved", options.equals("-i 1"));
                return null;
            }
        };
        storeDummyManager(sauceConnectFourManager);
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setOptions("-i ${BUILD_NUMBER}");

        Build build = runFreestyleBuild(sauceBuildWrapper);
        jenkinsRule.assertBuildStatusSuccess(build);


        //assert that the Sauce REST API was invoked for the Sauce job id
//        assertNotNull(restUpdates.get(currentSessionId));
        //TODO verify that test results of build include Sauce results

    }

    /**
     * Verifies that common options are set when the build is run.
     *
     * @throws Exception
     */
    @Test
    public void commonOptions() throws Exception {
        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                assertEquals("Variables are resolved correctly", options, "-i 1");
                return null;
            }
        };
        storeDummyManager(sauceConnectFourManager);
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        PluginImpl.get().setSauceConnectOptions("-i ${BUILD_NUMBER}");
        sauceBuildWrapper.setOptions("");

        Build build = runFreestyleBuild(sauceBuildWrapper);
        jenkinsRule.assertBuildStatusSuccess(build);


        //assert that the Sauce REST API was invoked for the Sauce job id
        //assertNotNull(restUpdates.get(currentSessionId));
        //TODO verify that test results of build include Sauce results

    }

    /**
     * Simulates the handling of a Sauce Connect time out.
     *
     * @throws Exception
     */
    @Test
    public void sauceConnectTimeOut() throws Exception {
        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                throw new SauceConnectDidNotStartException("Sauce Connect failed to start");
            }
        };
        storeDummyManager(sauceConnectFourManager);
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);

        FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build);
    }

    /**
     * Simulates the running of a build with Sauce Connect v4.
     *
     * @throws Exception thrown if an unexpected error occurs
     */
    @Test
    public void runSauceConnectVersion4() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);

        Build build = runFreestyleBuild(sauceBuildWrapper);
        jenkinsRule.assertBuildStatusSuccess(build);

        //assert that the Sauce REST API was invoked for the Sauce job id
//        assertNotNull(restUpdates.get(currentSessionId));
        //TODO verify that test results of build include Sauce results
        //assert that mock SC stopped
//        verify(sauceConnectFourManager.closeTunnelsForPlan(anyString(), anyString(), any(PrintStream.class)));


    }

    /**
     * Runs a basic build on the slave
     */
    /*@Test
    @Ignore("sauceBuildWrapper looses stubs on the slave")
    public void runSlaveBuild() throws Exception {
        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);

        DumbSlave s = jenkinsRule.createOnlineSlave();
        sauceBuildWrapper.setLaunchSauceConnectOnSlave(true);
        Build build = runFreestyleBuild(sauceBuildWrapper, null, s);
        jenkinsRule.assertBuildStatusSuccess(build);
    }*/


    /**
     * @throws Exception thrown if an unexpected error occurs
     */
    @Test
    public void multipleBrowsers() throws Exception {

        SauceOnDemandBuildWrapper sauceBuildWrapper = new TestSauceOnDemandBuildWrapper(credentialsId);
        sauceBuildWrapper.setWebDriverBrowsers(Arrays.asList("", ""));  /// THIS Actually crashes the buld but things are not properly checked

        SauceBuilder sauceBuilder = new SauceBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                //verify that SAUCE_ environment variables are populated
                Map<String, String> envVars = build.getEnvironment(listener);
                assertNotNull("Environment variable not found", envVars.get("SAUCE_ONDEMAND_BROWSERS"));
                return super.perform(build, launcher, listener);
            }
        };

        Build build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder);
        //jenkinsRule.assertBuildStatusSuccess(build);

        //assert that the Sauce REST API was invoked for the Sauce job id
//        assertNotNull(restUpdates.get(currentSessionId));
        //TODO verify that test results of build include Sauce results

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
        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
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
        jenkinsRule.assertBuildStatusSuccess(build);

        assertThat("greater than 0", holder.getInt("port"), greaterThan(0));
        assertEquals("Port provided to SC is the same as generated", holder.getInt("scProvidedPort"), holder.getInt("port"));
        assertEquals("Successful Build", build.getResult(), Result.SUCCESS);

        //assert that the Sauce REST API was invoked for the Sauce job id
//        assertNotNull(restUpdates.get(currentSessionId));
        //TODO verify that test results of build include Sauce results

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
        SauceConnectFourManager sauceConnectFourManager = new SauceConnectFourManager() {
            @Override
            public Process openConnection(String username, String apiKey, int port, File sauceConnectJar, String options,  PrintStream printStream, Boolean verboseLogging, String sauceConnectPath) throws SauceConnectException {
                holder.element("scProvidedPort", Integer.toString(port,10));
                return null;
            }
        };

        storeDummyManager(sauceConnectFourManager);
        SauceBuilder sauceBuilder = new SauceBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                Map<String, String> envVars = build.getEnvironment(listener);
                holder.element("env", envVars);
                return super.perform(build, launcher, listener);
            }
        };

        FreeStyleBuild build = runFreestyleBuild(sauceBuildWrapper, sauceBuilder);
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("Port Provided as ENV equals port started up on", port, holder.getString("scProvidedPort"));
        Map<String, String> envVars = (Map<String, String>)holder.get("env");
        assertNotNull(envVars);
        assertEquals("Port Provided as ENV equals SELENIUM_PORT", port, envVars.get("SELENIUM_PORT"));
    }

    private FreeStyleBuild runFreestyleBuild(SauceOnDemandBuildWrapper sauceBuildWrapper) throws Exception {
        return runFreestyleBuild(sauceBuildWrapper, null);
    }

    private FreeStyleBuild runFreestyleBuild(SauceOnDemandBuildWrapper sauceBuildWrapper, TestBuilder builder) throws Exception {
        return runFreestyleBuild(sauceBuildWrapper, builder, null);

    }

    /* FIXME - move to setup() */
    private FreeStyleBuild runFreestyleBuild(SauceOnDemandBuildWrapper sauceBuildWrapper, TestBuilder builder, Node node) throws Exception {

        if (builder == null) {
            builder = new SauceBuilder();
        }
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        if (node != null) {
            freeStyleProject.setAssignedNode(node);
        }
        freeStyleProject.getBuildWrappersList().add(sauceBuildWrapper);
        freeStyleProject.getBuildersList().add(builder);
        SauceOnDemandReportPublisher publisher = new SauceOnDemandReportPublisher() {
            @Override
            protected SauceREST getSauceREST(AbstractBuild build) {
                return spySauceRest;
            }
        };
        DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(freeStyleProject);
        testDataPublishers.add(publisher);
        JUnitResultArchiver resultArchiver = new JUnitResultArchiver("*.xml");
        resultArchiver.setKeepLongStdio(false);
//        JUnitResultArchiver resultArchiver = new JUnitResultArchiver("*.xml", false, testDataPublishers);
//        freeStyleProject.getPublishersList().add(resultArchiver);
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
            SauceCredentialsImpl credentials = SauceOnDemandBuildWrapper.getCredentials(build);
            //assert that mock SC started

            Map<String, String> envVars = build.getEnvironment(listener);

            assertEquals("Environment variable not found", credentials.getUsername(), envVars.get("SAUCE_USER_NAME"));
            assertEquals("Environment variable not found", credentials.getApiKey().getPlainText(), envVars.get("SAUCE_API_KEY"));

            File destination = new File(build.getWorkspace().getRemote(), "test.xml");
            FileUtils.copyURLToFile(getClass().getResource(currentTestResultFile), destination);
            return true;
        }
    }

}
