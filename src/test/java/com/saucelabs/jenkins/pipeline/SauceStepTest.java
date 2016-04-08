package com.saucelabs.jenkins.pipeline;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.hudson.HudsonSauceManagerFactory;
import hudson.model.Result;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.slaves.DumbSlave;
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
        DumbSlave s = r.createSlave();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('" + s.getNodeName() + "') { sauce('" + credentialsId + "') { \n" +
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
    public void sauceConnectTest() throws Exception {
        String credentialsId = SauceCredentials.migrateToCredentials("fakeuser", "fakekey", "unittest");

        SauceConnectFourManager sauceConnectFourManager = Mockito.mock(SauceConnectFourManager.class);

        storeDummyManager(sauceConnectFourManager);
        // stubbing appears before the actual execution
        Mockito.when(sauceConnectFourManager.openConnection(
            Mockito.eq("fakeuser"), Mockito.eq("fakekey"), Mockito.anyInt(),
            Mockito.any(File.class), Mockito.anyString(), Mockito.any(PrintStream.class),
            Mockito.eq(true), Mockito.anyString()
        )).thenReturn(null);

        DumbSlave s = r.createSlave();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('" + s.getNodeName() + "') { sauce('" + credentialsId + "') { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true) { \n" +
                "echo 'USERNAME=' + env.SAUCE_USERNAME\n" +
                "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n" +
            "}}}",
            true
        ));
        WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        r.assertLogContains("USERNAME=fakeuser", run);
        r.assertLogContains("ACCESS_KEY=fakekey", run);

        Mockito.verify(sauceConnectFourManager);
    }

    @Test
    public void sauceConnectWithoutSauceTest() throws Exception {
        DumbSlave s = r.createSlave();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('" + s.getNodeName() + "') { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true) { \n" +
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