package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class StopJobThread implements Runnable {
    private JobInformation job;
    private SauceREST sauceREST;

    public StopJobThread(SauceREST sauceREST, JobInformation job){
        this.job = job;
        this.sauceREST = sauceREST;
    }

    @Override
    public void run() {
        this.sauceREST.stopJob(job.getJobId());
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

    // we can grab the buildName from a job, or possibly from sanitized build name
    @Exported(visibility=2)
    public JenkinsBuildInformation getSauceBuild() {
        if (buildInformation == null) {
            try {
                buildInformation = retrieveBuildFromSauce(getSauceREST(), build);
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }

        return buildInformation;
    }

    public static JenkinsBuildInformation retrieveBuildFromSauce(SauceREST sauceREST, Run build) throws JSONException {
        // this is the build name in the sauce API
        String buildNumber = SauceEnvironmentUtil.getSanitizedBuildNumber(build);

        JenkinsBuildInformation buildInformation = new JenkinsBuildInformation(buildNumber);

        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);
        String jsonResponse = sauceREST.getBuild(buildNumber);
        JSONObject buildObj = new JSONObject(jsonResponse);

        if (buildObj == null) {
            logger.log(Level.WARNING, "Unable to find build data for " + buildNumber);
        } else {
            buildInformation.populateFromJson(buildObj);
        }
        return buildInformation;
    }


    @Exported(visibility=2)
    public List<JenkinsJobInformation> getJobs() {
        if (jobInformation == null) {
            try {
                jobInformation = new ArrayList<JenkinsJobInformation>();
                jobInformation.addAll(retrieveJobIdsFromSauce(getSauceREST(), build, getCredentials()).values());
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }
        SauceCredentials credentials = getCredentials();
        for (JobInformation j : jobInformation) {
            j.setHmac(credentials.getHMAC(j.getJobId()));

        }
        return jobInformation;
    }

    // Get the list of running jobs and stop them all
    public void stopJobs() {
        SauceREST sauceREST = getSauceREST();
        List<JenkinsJobInformation> jobs = getJobs();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (JobInformation job : jobs) {
            Runnable worker = new StopJobThread(sauceREST, job);
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

    // Get the list of jobs and update them with custom data
    public void updateJobs(Map<String, Object> customDataObj) {
        SauceREST sauceREST = getSauceREST();
        List<JenkinsJobInformation> jobs = getJobs();
        for (JobInformation job : jobs) {
            sauceREST.updateJobInfo(job.getJobId(), customDataObj);
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
    public static LinkedHashMap<String, JenkinsJobInformation> retrieveJobIdsFromSauce(SauceREST sauceREST, Run build) throws JSONException {
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


    public static LinkedHashMap<String, JenkinsJobInformation> retrieveJobIdsFromSauce(SauceREST sauceREST, Run build, SauceCredentials credentials) throws JSONException {
        //invoke Sauce Rest API to find plan results with those values
        LinkedHashMap<String, JenkinsJobInformation> jobInformation = new LinkedHashMap<String, JenkinsJobInformation>();

        String buildNumber = SauceEnvironmentUtil.getSanitizedBuildNumber(build);
        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);
        String jsonResponse = sauceREST.getBuildFullJobs(buildNumber, 5000);
        JSONObject job = new JSONObject(jsonResponse);
        JSONArray jobResults = job.getJSONArray("jobs");
        if (jobResults == null) {
            logger.log(Level.WARNING, "Unable to find job data for " + buildNumber);

        } else {
            //the list of results retrieved from the Sauce REST API is last-first, so reverse the list
            for (int i = jobResults.length() - 1; i >= 0; i--) {
                //check custom data to find job that was for build
                JSONObject jobData = jobResults.getJSONObject(i);
                String jobId = jobData.getString("id");
                JenkinsJobInformation information = new JenkinsJobInformation(jobId, credentials.getHMAC(jobId));
                information.populateFromJson(jobData);
                jobInformation.put(information.getJobId(), information);
            }
        }
        return jobInformation;
    }

    public Map<String,String> getAnalytics() {
        HashMap<String,String> analytics = new HashMap<String,String>();

        List<JenkinsJobInformation> allJobs = getJobs();
        int maxJobDuration = 0;
        for (JenkinsJobInformation job : allJobs) {
            int duration = job.getDuration();
            if (duration > maxJobDuration) {
                maxJobDuration = duration;
            }
        }

        analytics.put("start", buildInformation.getStartDate());
        analytics.put("duration", buildInformation.getPrettyDuration());
        analytics.put("efficiency", buildInformation.getEfficiency(maxJobDuration));
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
        return new JenkinsSauceREST(username, accessKey);
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
