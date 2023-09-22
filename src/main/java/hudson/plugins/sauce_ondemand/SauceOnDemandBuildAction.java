package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.JobSource;
import com.saucelabs.saucerest.api.BuildsEndpoint;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.model.builds.*;
import hudson.Util;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Job;
import hudson.model.Run;
import hudson.maven.MavenBuild;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.Timer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class StopJobThread implements Runnable {
    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildAction.class.getName());

    private JobInformation job;
    private JobsEndpoint jobs;

    public StopJobThread(JenkinsSauceREST sauceREST, JobInformation job){
        this.job = job;
        this.jobs = sauceREST.getJobsEndpoint();
    }

    @Override
    public void run() {
        try {
            jobs.stopJob(job.getJobId());
        } catch (IOException e) {
            // Ignore stopped job
            logger.log(Level.WARNING, "Failed to stop job " + job.getJobId() + ": " + e);
        }
    }
}

/**
 * Presents the links to the Sauce OnDemand jobs on the build summary page.
 *
 * @author Ross Rowe
 */
@ExportedBean
public class SauceOnDemandBuildAction extends AbstractAction implements Serializable, RunAction2, SimpleBuildStep.LastBuildAction {
    private static final long serialVersionUID = 1L;

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildAction.class.getName());

    /**
     * Regex pattern that is used to identify Sauce job ids which have been run as part of a Jenkins build.
     */
    public static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)(?:.job-name=(.*))?");

    private transient Run build;
    private List<JenkinsJobInformation> jobInformation;
    private JenkinsBuildInformation buildInformation;

    @Deprecated
    private String accessKey;
    @Deprecated
    private String username;

    private String credentialsId;

    private String restEndpoint;

    @DataBoundConstructor
    public SauceOnDemandBuildAction(Run build, String credentialsId) {
        this.credentialsId = credentialsId;
        this.build = build;
    }

    public Run getBuild() {
        return build;
    }

    public boolean hasSauceOnDemandResults() {
        if (jobInformation == null) {
            return false;
        }
        return !getJobs().isEmpty();
    }

    /**
     * Default method of getting Sauce build information using the sanitized Jenkins build number
     */
    @Exported(visibility=2)
    public JenkinsBuildInformation getSauceBuild(boolean updateBuild) {
        if (updateBuild || buildInformation == null) {
            String buildNumber = SauceEnvironmentUtil.getSanitizedBuildNumber(build);
            buildInformation = getSauceBuild(buildNumber, updateBuild);
        }
        return buildInformation;
    }

    @Exported(visibility=2)
    public JenkinsBuildInformation getSauceBuild() {
        return getSauceBuild(false);
    }

    /**
     * Method for getting Sauce build information if we know the actual Sauce build name
     */
    @Exported(visibility=2)
    public JenkinsBuildInformation getSauceBuild(String sauceBuildName, boolean updateBuild) {
        if (updateBuild || buildInformation == null) {
            try {
                buildInformation = retrieveBuildFromSauce(getSauceREST(), sauceBuildName);
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }

        return buildInformation;
    }

    @Exported(visibility=2)
    public JenkinsBuildInformation getSauceBuild(String sauceBuildName) {
        return getSauceBuild(sauceBuildName, false);
    }

    /**
     * Invokes the Sauce REST API to retrieve the build information.
     * @param sauceREST    Sauce Rest object/credentials to use
     * @param buildNumber  The build name on Sauce or sanitized build number from Jenkins
     * @return Jenkins build information
     * @throws JSONException Not json returned properly
     */
    public static JenkinsBuildInformation retrieveBuildFromSauce(JenkinsSauceREST sauceREST, String buildNumber) throws JSONException {
        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);

        // A note on retry behaviour
        // This code was previously set to retry 10 times with 10 seconds pause in between - this resulted
        // in de-facto hang behaviour in Jenkins on the status pages of builds if an error occurred, which
        // meant either that the page took over 100 seconds to load, or that the connection was killed by
        // a downstream proxy (e.g. an ELB) which had a connection timeout below 100 seconds. It has been
        // updated to retry one time with a 3 second pause to allow for recovery from brief blips while
        // not stalling page load for more than a few seconds to ensure a good user experience.
        int retries = 0;
        int maxRetries = 1;
        String jsonResponse = "";

        while (retries < maxRetries && "".equals(jsonResponse)) {
            try {
                JenkinsBuildInformation buildInformation = retrieveBuildInformationFromSauce(sauceREST, buildNumber);
                if (!"".equals(buildInformation.getBuildId())) {
                    return buildInformation;
                }
            } catch (Exception e) {
                jsonResponse = "";
            }
            logger.log(Level.WARNING, "Sauce REST API get build JSON Response was empty or threw an exception for " + buildNumber + ", waiting and retrying");
            retries++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
        }

        return new JenkinsBuildInformation("");
    }

    /**
     * Invokes the Sauce REST API to retrieve the build information.
     * @param sauceREST    Sauce Rest object/credentials to use
     * @param buildNumber  The build name on Sauce or sanitized build number from Jenkins
     * @return Jenkins build information
     * @throws JSONException Unable to parse json
     */
    public static JenkinsBuildInformation retrieveBuildInformationFromSauce(
            JenkinsSauceREST sauceREST, String buildNumber)
            throws JSONException, IOException {
        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);

	BuildsEndpoint buildsEndpoint = sauceREST.getBuildsEndpoint();

	LookupBuildsParameters parameters = new LookupBuildsParameters.Builder()
		.setName(buildNumber)
		.setLimit(1)
		.build();

	List<Build> builds = buildsEndpoint.lookupBuilds(JobSource.VDC, parameters);

        if (builds == null || builds.size() == 0) {
            logger.warning("Unable to find build for name: `" + buildNumber + "`");
            return new JenkinsBuildInformation("");
        }

        JenkinsBuildInformation buildInformation = new JenkinsBuildInformation(builds.get(0));
        return buildInformation;
    }

    @Exported(visibility=2)
    public List<JenkinsJobInformation> getJobs(boolean updateJobs) {
        if (updateJobs || jobInformation == null) {
            try {
                jobInformation = new ArrayList<JenkinsJobInformation>();
                jobInformation.addAll(retrieveJobIdsFromSauce(getSauceREST(), build, getCredentials()).values());
            } catch (JSONException|IOException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }
        SauceCredentials credentials = getCredentials();
        for (JobInformation j : jobInformation) {
            j.setHmac(credentials.getHMAC(j.getJobId()));

        }
        return jobInformation;
    }

    @Exported(visibility=2)
    public List<JenkinsJobInformation> getJobs() {
        return getJobs(false);
    }

    // Get the list of running jobs and stop them all
    public void stopJobs() throws InterruptedException {
        JenkinsSauceREST sauceREST = getSauceREST();
        List<JenkinsJobInformation> jobs = getJobs();
        List<Future<?>> futures = new ArrayList<>();
        for (JobInformation job : jobs) {
            Runnable worker = new StopJobThread(sauceREST, job);
            futures.add(Timer.get().submit(worker));
        }
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException x) {
                logger.log(Level.WARNING, "Could not stop a job", x);
            }
        }
    }

    // Get the list of jobs and update them with custom data
    public void updateJobs(Map<String, String> customDataObj) throws IOException {
        JenkinsSauceREST sauceREST = getSauceREST();
	JobsEndpoint jobEndpoint = sauceREST.getJobsEndpoint();
        List<JenkinsJobInformation> jobs = getJobs();
        for (JobInformation job : jobs) {
            jobEndpoint.addCustomData(job.getJobId(), customDataObj);
        }
    }

    @Override
    protected SauceCredentials getCredentials() {
        if (credentialsId != null) {
            return SauceCredentials.getCredentialsById(build.getParent(), credentialsId);
        } else if (build instanceof AbstractBuild) {
            return SauceCredentials.getCredentials((AbstractBuild) build);
        }
        return null;
    }

    /**
     * Invokes the Sauce REST API to retrieve the details for the jobs the user has access to.  Iterates over the jobs
     * and attempts to find the job that has a 'build' field matching the build key/number.
     * @param sauceREST    Sauce Rest object/credentials to use
     * @param build        Which build this is requesting job ids from
     * @return List of processed job information
     * @throws JSONException Not json returned properly
     */
    public static LinkedHashMap<String, JenkinsJobInformation> retrieveJobIdsFromSauce(
            JenkinsSauceREST sauceREST, Run build)
            throws JSONException, IOException {
        SauceCredentials credentials = getSauceBuildAction(build).getCredentials();
        return retrieveJobIdsFromSauce(sauceREST, build, credentials);
    }

    /**
     * @param build The build in progress
     * @return the {@link SauceOnDemandBuildAction} instance which has been registered with the build
     *         Can be null
     */
    public static SauceOnDemandBuildAction getSauceBuildAction(Run build) {
        if (build == null) { return null; }
        SauceOnDemandBuildAction buildAction = build.getAction(SauceOnDemandBuildAction.class);
        if (buildAction == null && build instanceof MavenBuild) {
            MavenModuleSetBuild mb = ((MavenBuild) build).getParentBuild();
            if (mb != null) {
                //try the parent
                buildAction = mb.getAction(SauceOnDemandBuildAction.class);
            }
        }
        return buildAction;
    }


    public static LinkedHashMap<String, JenkinsJobInformation> retrieveJobIdsFromSauce(
            JenkinsSauceREST sauceREST, Run build, SauceCredentials credentials)
            throws JSONException, IOException {

        logger.log(Level.WARNING, "In retrieveJobIdsFromSauce: " + build);

        //invoke Sauce Rest API to find plan results with those values
        LinkedHashMap<String, JenkinsJobInformation> jobInformation = new LinkedHashMap<String, JenkinsJobInformation>();

        String buildNumber = SauceEnvironmentUtil.getSanitizedBuildNumber(build);
        JenkinsBuildInformation buildInformation = SauceOnDemandBuildAction.retrieveBuildInformationFromSauce(sauceREST, buildNumber);
        String buildId = buildInformation.getBuildId();
        if ("".equals(buildId))
            return jobInformation;
        List<String> jobIds = SauceOnDemandBuildAction.getJobIdsForBuild(sauceREST, buildId);
        Map<String, JenkinsJobInformation> jobs = SauceOnDemandBuildAction.getJobsInformation(sauceREST, credentials, jobIds);
        for (String jobId: jobIds) {
            JenkinsJobInformation information = jobs.get(jobId);
            if (information != null) {
                jobInformation.put(jobId, information);
            }
        }
        return jobInformation;
    }

    protected static List<String> getJobIdsForBuild(JenkinsSauceREST sauceREST, String buildId) {
        List<String> jobIds = new ArrayList<String>();

        LookupJobsParameters parameters = new LookupJobsParameters.Builder()
            .build();

        BuildsEndpoint buildsEndpoint = sauceREST.getBuildsEndpoint();
        try {
            List<com.saucelabs.saucerest.model.jobs.Job> jobs = buildsEndpoint.lookupJobsForBuild(JobSource.VDC, buildId, parameters);

            if (jobs == null || jobs.size() == 0) {
                logger.log(Level.WARNING, "Build without jobs id=`" + buildId + "`");
                return jobIds;
            }

            for (int i = 0; i < jobs.size(); i++) {
                jobIds.add(jobs.get(i).id);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to retrieve jobs for build " + buildId);
            return jobIds;
        }

        return jobIds;
    }

    protected static Map<String, JenkinsJobInformation> getJobsInformation(
            JenkinsSauceREST sauceREST, SauceCredentials credentials, Iterable<String> jobIds)
            throws JSONException, IOException {
        Map<String, JenkinsJobInformation> jobs = new HashMap<String, JenkinsJobInformation>();
	JobsEndpoint jobsEndpoint = sauceREST.getJobsEndpoint();

        List<List<String>> slicedIds = SauceOnDemandBuildAction.slice(jobIds, 20);

        for (List<String> slice: slicedIds) {
            List<com.saucelabs.saucerest.model.jobs.Job> jobResults = jobsEndpoint.getJobDetails(slice);

            for (int i = 0; i < jobResults.size(); i++) {
                com.saucelabs.saucerest.model.jobs.Job job = jobResults.get(i);

                JenkinsJobInformation information = new JenkinsJobInformation(job.id, credentials.getHMAC(job.id));
                information.populate(job);
                jobs.put(information.getJobId(), information);
            }
        }

        return jobs;
    }

    protected static List<List<String>> slice(Iterable<String> strings, int sliceSize) {
        List<List<String>> sliced = new ArrayList<List<String>>();
        List<String> current = null;
        for (String s: strings) {
            if (current == null || current.size() >= sliceSize) {
                current = new ArrayList<String>();
                sliced.add(current);
            }
            current.add(s);
        }
        return sliced;
    }

    public Map<String,String> getAnalytics() {
        logger.fine("Getting Sauce analytics");
        HashMap<String,String> analytics = new HashMap<String,String>();

        JenkinsBuildInformation buildInformation = getSauceBuild(true);
        List<JenkinsJobInformation> allJobs = getJobs();
        long maxJobDuration = 0;
        long totalJobDuration = 0;
        for (JenkinsJobInformation job : allJobs) {
            long duration = job.getDuration();
            totalJobDuration += duration;
            if (duration > maxJobDuration) {
                maxJobDuration = duration;
            }
        }

        analytics.put("start", buildInformation.getStartDate());
        analytics.put("duration", buildInformation.getPrettyDuration());
        analytics.put("efficiency", buildInformation.getEfficiency(maxJobDuration, totalJobDuration));
        analytics.put("size", String.valueOf(buildInformation.getJobsFinished()));
        analytics.put("pass", buildInformation.getJobsPassRate());
        analytics.put("fail", buildInformation.getJobsFailRate());
        analytics.put("error", buildInformation.getJobsErrorRate());

        return analytics;
    }

    protected JenkinsSauceREST getSauceREST() {
        SauceCredentials creds = getCredentials();
        String username = creds != null ? creds.getUsername() : null;
        String accessKey = creds != null ? creds.getPassword().getPlainText() : null;
        String dataCenter = creds != null ? creds.getRestEndpointName() : null;

	DataCenter dc = DataCenter.fromString(dataCenter);

        JenkinsSauceREST sauceREST = new JenkinsSauceREST(username, accessKey, dc);
        return sauceREST;
    }

    public SauceTestResultsById getById(String id) {
        return new SauceTestResultsById(id, getCredentials());
    }

    /**
     *
     * @param req Standard Request Object
     * @param rsp Standard Response Object
     * @throws IOException Unable to load index.jelly template
     */
    @SuppressWarnings("unused") // used by stapler
    public void doJobReport(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        SauceTestResultsById byId = getById(req.getParameter("jobId"));
        try {
            req.getView(byId, "index.jelly").forward(req, rsp);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    public void setJobs(List<JenkinsJobInformation> jobs) {
        this.jobInformation = jobs;
    }

    protected Object readResolve() {
        if (credentialsId == null) {
            if (build.getParent() instanceof BuildableItemWithBuildWrappers) {
                BuildableItemWithBuildWrappers p = (BuildableItemWithBuildWrappers) build.getParent();
                SauceOnDemandBuildWrapper bw = p.getBuildWrappersList().get(SauceOnDemandBuildWrapper.class);
                this.credentialsId = bw.getCredentialId();
            }
        }
        return this;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        Job<?,?> job = build.getParent();
        if (/* getAction(Class) produces a StackOverflowError */!Util.filter(job.getActions(), SauceOnDemandProjectAction.class).isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.singleton(new SauceOnDemandProjectAction(job));
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.build = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.build = run;
    }
}