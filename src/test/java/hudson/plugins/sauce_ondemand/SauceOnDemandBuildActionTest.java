package hudson.plugins.sauce_ondemand;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.saucelabs.saucerest.JobSource;
import com.saucelabs.saucerest.api.BuildsEndpoint;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.api.SauceConnectEndpoint;
import com.saucelabs.saucerest.model.builds.Build;
import com.saucelabs.saucerest.model.builds.JobInBuild;
import com.saucelabs.saucerest.model.builds.JobsInBuild;
import com.saucelabs.saucerest.model.builds.LookupBuildsParameters;
import com.saucelabs.saucerest.model.builds.LookupJobsParameters;
import com.saucelabs.saucerest.model.jobs.Job;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.plugins.sauce_ondemand.mocks.MockSauceREST;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SauceOnDemandBuildActionTest {
  @ClassRule public static JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void doJobReportTest() throws Exception {
    final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
    final JobsEndpoint mockJobsEndpoint = mock(JobsEndpoint.class);
    final SauceConnectEndpoint mockSauceConnectEndpoint = mock(SauceConnectEndpoint.class);

    List<Job> jobs = makeBuildJobsResponse();
    when(mockJobsEndpoint.getJobDetails(anyList())).thenReturn(jobs);
    when(mockJobsEndpoint.getJobDetails(anyString())).thenReturn(jobs.get(0));
    when(mockSauceConnectEndpoint.getTunnelsForAUser()).thenReturn(Collections.emptyList());

    when(mockSauceREST.getJobsEndpoint()).thenReturn(mockJobsEndpoint);
    when(mockSauceREST.getSauceConnectEndpoint()).thenReturn(mockSauceConnectEndpoint);

    TestSauceOnDemandBuildWrapper bw =
        new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest"),
            mockSauceREST);
    bw.setEnableSauceConnect(false);

    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
    freeStyleProject.getBuildWrappersList().add(bw);
    FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
    SauceOnDemandBuildAction buildAction =
        new SauceOnDemandBuildAction(build, bw.getCredentialId()) {
          @Override
          protected JenkinsSauceREST getSauceREST() {
            return mockSauceREST;
          }
        };
    build.addAction(buildAction);

    HtmlPage page;
    try (JenkinsRule.WebClient webClient = jenkins.createWebClient()) {
      webClient.setJavaScriptEnabled(false);
      page =
          webClient.getPage(
              build, "sauce-ondemand-report/jobReport?jobId=5f119101b8b14db89b25250bf33341d7");
    }
    jenkins.assertGoodStatus(page);

    DomElement scriptTag = getEmbedTag(page.getElementsByTagName("iframe"));
    assertNotNull(scriptTag);
    assertThat(
        new URL(scriptTag.getAttribute("src")).getPath(),
        endsWith("/job-embed/5f119101b8b14db89b25250bf33341d7"));
    assertThat(new URL(scriptTag.getAttribute("src")).getQuery(), containsString("auth="));
  }

  @Test
  public void testGetSauceBuildAction() throws Exception {
    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
    TestSauceOnDemandBuildWrapper bw =
        new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest"));
    bw.setEnableSauceConnect(false);
    freeStyleProject.getBuildWrappersList().add(bw);
    FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
    SauceOnDemandBuildAction buildAction =
        new SauceOnDemandBuildAction(build, bw.getCredentialId());
    build.addAction(buildAction);
    SauceOnDemandBuildAction sauceBuildAction = SauceOnDemandBuildAction.getSauceBuildAction(build);
    assertEquals("fakeuser", sauceBuildAction.getCredentials().getUsername());
  }

  @Test
  public void testGetSauceBuildActionMavenBuild() throws Exception {
    MavenModuleSet project =
        jenkins.createProject(MavenModuleSet.class, "testGetSauceBuildActionMavenBuild");
    TestSauceOnDemandBuildWrapper bw =
        new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest"));
    project.getBuildWrappersList().add(bw);
    project.setScm(
        new SingleFileSCM("pom.xml", Objects.requireNonNull(getClass().getResource("/pom.xml"))));
    project.setGoals("clean");
    MavenModuleSetBuild build = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
    SauceOnDemandBuildAction buildAction =
        new SauceOnDemandBuildAction(build, bw.getCredentialId());
    build.addAction(buildAction);
    final MavenBuild mavenBuildMock = mock(MavenBuild.class);
    when(mavenBuildMock.getParentBuild()).thenReturn(build);
    SauceOnDemandBuildAction sauceBuildAction =
        SauceOnDemandBuildAction.getSauceBuildAction(mavenBuildMock);
    assertEquals("fakeuser", sauceBuildAction.getCredentials().getUsername());
  }

  @Test
  public void testRetrieveJobIdsFromSauce() throws Exception {
    final FreeStyleBuild build = makeMavenBuild();
    final SauceCredentials credentials = makeSauceCredentials();
    final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
    final List<String> jobIds = makeJobIds(4);
    final JobsInBuild buildJobsList = makeJobsResponse(jobIds);
    final List<Job> jobsResponseList = makeJobListResponse(jobIds);
    final List<Build> builds = makeBuildsByNameResponse();
    final BuildsEndpoint mockBuildsEndpoint = mock(BuildsEndpoint.class);
    final JobsEndpoint mockJobsEndpoint = mock(JobsEndpoint.class);

    when(mockBuildsEndpoint.lookupJobsForBuild(
            any(JobSource.class), anyString(), any(LookupJobsParameters.class)))
        .thenReturn(buildJobsList);

    when(mockBuildsEndpoint.lookupBuilds(any(JobSource.class), any(LookupBuildsParameters.class)))
        .thenReturn(builds);

    when(mockBuildsEndpoint.lookupJobsForBuild(
            any(JobSource.class), anyString(), any(LookupJobsParameters.class)))
        .thenReturn(buildJobsList);

    when(mockJobsEndpoint.getJobDetails(anyList())).thenReturn(jobsResponseList);

    when(mockSauceREST.getBuildsEndpoint()).thenReturn(mockBuildsEndpoint);
    when(mockSauceREST.getJobsEndpoint()).thenReturn(mockJobsEndpoint);

    LinkedHashMap<String, JenkinsJobInformation> jobInformation =
        SauceOnDemandBuildAction.retrieveJobIdsFromSauce(mockSauceREST, build, credentials);

    Set<String> jobIdsSet = new HashSet<>(jobIds);
    assertEquals(jobInformation.keySet(), jobIdsSet);
  }

  @Test
  public void testRetrieveJobIdsFromSauceSplitsCallsToResto() throws Exception {
    final FreeStyleBuild build = makeMavenBuild();
    final SauceCredentials credentials = makeSauceCredentials();
    final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
    final int jobCount = 30;
    final List<String> jobIds = makeJobIds(jobCount);
    final JobsInBuild buildJobsList = makeJobsResponse(jobIds);
    final List<Job> jobsResponse1 = makeJobListResponse(jobIds.subList(0, 20));
    final List<Job> jobsResponse2 = makeJobListResponse(jobIds.subList(20, jobIds.size()));
    final List<Build> buildList = makeBuildsByNameResponse();
    final BuildsEndpoint mockBuildsEndpoint = mock(BuildsEndpoint.class);
    final JobsEndpoint mockJobsEndpoint = mock(JobsEndpoint.class);

    when(mockBuildsEndpoint.lookupBuilds(any(JobSource.class), any(LookupBuildsParameters.class)))
        .thenReturn(buildList);

    when(mockBuildsEndpoint.lookupJobsForBuild(
            any(JobSource.class), anyString(), any(LookupJobsParameters.class)))
        .thenReturn(buildJobsList);

    when(mockJobsEndpoint.getJobDetails(anyList()))
        .thenReturn(jobsResponse1)
        .thenReturn(jobsResponse2);

    when(mockSauceREST.getBuildsEndpoint()).thenReturn(mockBuildsEndpoint);
    when(mockSauceREST.getJobsEndpoint()).thenReturn(mockJobsEndpoint);

    LinkedHashMap<String, JenkinsJobInformation> jobInformation =
        SauceOnDemandBuildAction.retrieveJobIdsFromSauce(mockSauceREST, build, credentials);

    Set<String> jobIdsSet = new HashSet<>(jobIds);
    assertEquals(jobInformation.keySet(), jobIdsSet);
  }

  @Test
  public void testRetrieveJobIdsFromSauceIfBuildIsNotFound() throws Exception {
    final FreeStyleBuild build = makeMavenBuild();
    final SauceCredentials credentials = makeSauceCredentials();
    final JenkinsSauceREST mockSauceREST = mock(MockSauceREST.class);
    final BuildsEndpoint mockBuildsEndpoint = mock(BuildsEndpoint.class);

    when(mockBuildsEndpoint.lookupJobsForBuild(
            any(JobSource.class), anyString(), any(LookupJobsParameters.class)))
        .thenReturn(new JobsInBuild());

    when(mockSauceREST.getBuildsEndpoint()).thenReturn(mockBuildsEndpoint);

    LinkedHashMap<String, JenkinsJobInformation> jobInformation =
        SauceOnDemandBuildAction.retrieveJobIdsFromSauce(mockSauceREST, build, credentials);

    assertEquals(jobInformation.keySet().size(), 0);
  }

  private List<String> makeJobIds(int jobCount) {
    List<String> jobIds = new ArrayList<>();
    for (int i = 0; i < jobCount; i++) jobIds.add(String.format("%032x", i + 1));
    return jobIds;
  }

  private SauceCredentials makeSauceCredentials() {
    return new SauceCredentials(
        CredentialsScope.GLOBAL, "credentials-id", "fakeuser", "fake-access-key", "localhost", "");
  }

  private FreeStyleBuild makeMavenBuild() throws Exception {
    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject();
    TestSauceOnDemandBuildWrapper bw =
        new TestSauceOnDemandBuildWrapper(
            SauceCredentials.migrateToCredentials("fakeuser", "fakekey", null, "unittest"));
    bw.setEnableSauceConnect(false);
    freeStyleProject.getBuildWrappersList().add(bw);
    return freeStyleProject.scheduleBuild2(0).get();
  }

  private List<Job> makeBuildJobsResponse() throws Exception {
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<Job> jobJsonAdapter = moshi.adapter(Job.class);
    List<Job> jobsResponse = new ArrayList<>();
    try (InputStream resourceAsStream =
        SauceOnDemandProjectActionTest.class.getResourceAsStream("/build_jobs.json")) {
      assertNotNull(resourceAsStream);
      String buildJobsV2Response = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      JSONObject jsonResponse = new JSONObject(buildJobsV2Response);
      JSONArray jobs = jsonResponse.getJSONArray("jobs");
      for (int i = 0; i < jobs.length(); i++) {
        JSONObject job = jobs.getJSONObject(i);
        jobsResponse.add(jobJsonAdapter.fromJson(job.toString()));
      }
      return jobsResponse;
    }
  }

  private List<Build> makeBuildsByNameResponse() throws Exception {
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<Build> buildJsonAdapter = moshi.adapter(Build.class);
    try (InputStream resourceAsStream =
        SauceOnDemandProjectActionTest.class.getResourceAsStream("/builds_by_name.json")) {
      String buildsByName =
          IOUtils.toString(Objects.requireNonNull(resourceAsStream), StandardCharsets.UTF_8);
      JSONObject jsonResponse = new JSONObject(buildsByName);
      JSONArray builds = jsonResponse.getJSONArray("builds");
      List<Build> buildList = new ArrayList<>();
      for (int i = 0; i < builds.length(); i++) {
        JSONObject build = builds.getJSONObject(i);
        buildList.add(buildJsonAdapter.fromJson(build.toString()));
      }
      return buildList;
    }
  }

  private JobsInBuild makeJobsResponse(List<String> ids) throws Exception {
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<JobsInBuild> jobsInBuildJsonAdapter = moshi.adapter(JobsInBuild.class);
    try (InputStream resourceAsStream =
        SauceOnDemandProjectActionTest.class.getResourceAsStream("/build_jobs_v2.json")) {
      assertNotNull(resourceAsStream);
      String buildJobsV2Response = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      JSONObject jsonResponse = new JSONObject(buildJobsV2Response);
      JobsInBuild jobsInBuild = jobsInBuildJsonAdapter.fromJson(jsonResponse.toString());
      assertNotNull(jobsInBuild);
      assertNotNull(jobsInBuild.jobs);
      JobInBuild jobInBuild = jobsInBuild.jobs.get(0);
      List<JobInBuild> jobs = new ArrayList<>();
      for (String jobId : ids) {
        JobInBuild clone =
            new JobInBuild(
                jobInBuild.creationTime,
                jobInBuild.deletionTime,
                jobId,
                jobInBuild.modificationTime,
                jobInBuild.state);
        jobs.add(clone);
      }
      jobsInBuild.jobs = jobs;
      return jobsInBuild;
    }
  }

  private List<Job> makeJobListResponse(List<String> ids) throws Exception {
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<Job> jobJsonAdapter = moshi.adapter(Job.class);
    try (InputStream resourceAsStream =
        SauceOnDemandProjectActionTest.class.getResourceAsStream("/jobs_by_ids.json")) {
      assertNotNull(resourceAsStream);
      String buildJobsV2Response = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      JSONArray jobs = new JSONArray(buildJobsV2Response);
      JSONObject job = jobs.getJSONObject(0);
      jobs.clear();
      List<Job> jobList = new ArrayList<>();
      for (String jobId : ids) {
        JSONObject cloned = new JSONObject(job.toMap());
        cloned.put("id", jobId);
        jobs.put(cloned);
        jobList.add(jobJsonAdapter.fromJson(cloned.toString()));
      }
      return jobList;
    }
  }

  private DomElement getEmbedTag(DomNodeList<DomElement> scripts) {
    for (DomElement htmlElement : scripts) {
      if (htmlElement.getAttribute("src").contains("job-embed")) {
        return htmlElement;
      }
    }
    return null;
  }
}
