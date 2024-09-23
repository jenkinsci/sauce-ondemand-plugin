package com.saucelabs.jenkins.pipeline;

import static org.mockito.ArgumentMatchers.isNull;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import com.saucelabs.jenkins.HudsonSauceManagerFactory;
import com.saucelabs.saucerest.DataCenter;

import hudson.model.Result;
import hudson.plugins.sauce_ondemand.PluginImpl;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

public class SauceStepTest {
  @ClassRule public static JenkinsRule r = new JenkinsRule();

  @Before
  public void setUp() throws Exception {
    PluginImpl p = PluginImpl.get();
    if (p != null) {
      p.setSauceConnectOptions("");
    }
  }

  private void storeDummyManager(SauceConnectManager sauceConnectManager) throws Exception {
    HudsonSauceManagerFactory factory = HudsonSauceManagerFactory.getInstance();
    Field field = HudsonSauceManagerFactory.class.getDeclaredField("sauceConnectManager");
    field.setAccessible(true);
    field.set(factory, sauceConnectManager);
  }

  @Test
  public void sauceTest() throws Exception {
    String credentialsId =
        SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest");
    WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "SauceStepTest-sauceTest");
    p.setDefinition(
        new CpsFlowDefinition(
            "node { sauce('"
                + credentialsId
                + "') { \n"
                + "echo 'USERNAME=' + env.SAUCE_USERNAME\n"
                + "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n"
                + "}}",
            true));
    WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
    r.assertLogContains("USERNAME=fakeuser", run);
    r.assertLogContains("ACCESS_KEY=fakekey", run);
  }

  @Test
  public void sauceJWTTest() throws Exception {
    String credentialsId =
        SauceCredentials.migrateToCredentials("fakeJWTuser", "fakeJWTkey", null, "unittest");
    WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "SauceStepTest-sauceJWTTest");
    SauceCredentials.getCredentialsById(p, credentialsId)
        .setShortLivedConfig(new SauceCredentials.ShortLivedConfig(120));

    p.setDefinition(
        new CpsFlowDefinition(
            "node { sauce('"
                + credentialsId
                + "') { \n"
                + "echo 'USERNAME=' + env.SAUCE_USERNAME\n"
                + "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n"
                + "}}",
            true));
    WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
    r.assertLogContains("USERNAME=fakeJWTuser", run);
    String[] lines = r.getLog(run).split("\n|\r");
    String accessKey = null;
    for (String line : lines) {
      if (line.contains("ACCESS_KEY=")) {
        accessKey = line.replaceFirst("ACCESS_KEY=", "");
      }
    }
    Assert.assertNotNull(accessKey);
    JWT.require(Algorithm.HMAC256("fakeJWTkey")).build().verify(accessKey);
  }

  @Test
  public void sauceConnectWithGlobalOptionsTest() throws Exception {
    String credentialsId =
        SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest");

    SauceConnectManager sauceConnectManager = Mockito.mock(SauceConnectManager.class);

    storeDummyManager(sauceConnectManager);

    // stubbing appears before the actual execution
    Mockito.when(
            sauceConnectManager.openConnection(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(DataCenter.class),
                Mockito.anyInt(),
                Mockito.any(File.class),
                Mockito.anyString(),
                Mockito.any(PrintStream.class),
                Mockito.eq(true),
                Mockito.anyString()))
        .thenReturn(null);
    PluginImpl.get().setSauceConnectOptions("-i gavin -vv");

    WorkflowJob p =
        r.jenkins.createProject(
            WorkflowJob.class, "SauceStepTest-sauceConnectWithGlobalOptionsTest");
    p.setDefinition(
        new CpsFlowDefinition(
            "node { sauce('"
                + credentialsId
                + "') { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true, options: '-i tunnel-name') { \n"
                + "echo 'USERNAME=' + env.SAUCE_USERNAME\n"
                + "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n"
                + "}}}",
            true));
    WorkflowRun run = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
    r.assertLogContains("USERNAME=fakeuser", run);
    r.assertLogContains("ACCESS_KEY=fakekey", run);

    Mockito.verify(sauceConnectManager)
        .openConnection(
            Mockito.eq("fakeuser"),
            Mockito.eq("fakekey"),
            Mockito.eq("US_WEST"),
            Mockito.anyInt(),
            isNull(),
            Mockito.matches(
                "-i gavin -vv -i tunnel-name --tunnel-name [a-zA-Z0-9_-]+ -x https://saucelabs.com/rest/v1"),
            Mockito.any(PrintStream.class),
            Mockito.eq(true),
            isNull());
  }

  @Test
  public void sauceConnectWithoutSauceTest() throws Exception {
    WorkflowJob p =
        r.jenkins.createProject(WorkflowJob.class, "SauceStepTest-sauceConnectWithoutSauceTest");
    p.setDefinition(
        new CpsFlowDefinition(
            "node { sauceconnect(useGeneratedTunnelIdentifier: true, verboseLogging: true) { \n"
                + "echo 'USERNAME=' + env.SAUCE_USERNAME\n"
                + "echo 'ACCESS_KEY=' + env.SAUCE_ACCESS_KEY\n"
                + "}}",
            true));
    WorkflowRun run = p.scheduleBuild2(0).get();
    r.assertBuildStatus(Result.FAILURE, run);
    r.assertLogContains(
        "Perhaps you forgot to surround the code with a step that provides this, such as: sauce",
        run);
  }
}
