package com.saucelabs.jenkins.pipeline;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import hudson.model.Result;
import hudson.plugins.sauce_ondemand.PluginImpl;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;

public class SauceStepTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        PluginImpl.get().setSauceConnectOptions("");
    }

    private void storeDummyManager(SauceConnectFourManager sauceConnectFourManager) throws Exception {
        HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
        Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectFourManager");
        field.setAccessible(true);
        field.set(factory, sauceConnectFourManager);
    }

    @Test
    public void sauceTest() throws Exception {
        String credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest");
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node { sauce('" + credentialsId + "') { \n" +
                "echo 'USERNAME=' + env.SAUCE_USERNAME\n" +
                "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n" +

                "}}",
            true
        ));
        WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        r.assertLogContains("USERNAME=fakeuser", run);
        r.assertLogContains("ACCESS_KEY=fakekey", run);
    }

    @Test
    public void sauceConnectWithGlobalOptionsTest() throws Exception {
        String credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest");

        SauceConnectFourManager sauceConnectFourManager = Mockito.mock(SauceConnectFourManager.class);

        storeDummyManager(sauceConnectFourManager);

        // stubbing appears before the actual execution
        Mockito.when(sauceConnectFourManager.openConnection(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.any(File.class),
            Mockito.anyString(),
            Mockito.any(PrintStream.class),
            Mockito.eq(true),
            Mockito.anyString()
        )).thenReturn(null);
        PluginImpl.get().setSauceConnectOptions("-i gavin -vv");

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node { sauce('" + credentialsId + "') { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true, options: '-i tunnel-ident') { \n" +
                "echo 'USERNAME=' + env.SAUCE_USERNAME\n" +
                "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n" +
                "}}}",
            true
        ));
        WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        r.assertLogContains("USERNAME=fakeuser", run);
        r.assertLogContains("ACCESS_KEY=fakekey", run);

        Mockito.verify(sauceConnectFourManager).openConnection(
            Mockito.eq("fakeuser"),
            Mockito.eq("fakekey"),
            Mockito.anyInt(),
            Mockito.any(File.class),
            Mockito.matches("-i gavin -vv -i tunnel-ident --tunnel-identifier [a-zA-Z0-9_-]+"),
            Mockito.any(PrintStream.class),
            Mockito.eq(true),
            Mockito.anyString()
        );
    }

    @Test
    public void sauceConnectWithoutSauceTest() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true) { \n" +
                "echo 'USERNAME=' + env.SAUCE_USERNAME\n" +
                "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n" +
                "}}",
            true
        ));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.assertBuildStatus(Result.FAILURE, run);
        r.assertLogContains("Perhaps you forgot to surround the code with a step that provides this, such as: sauce", run);
    }
}