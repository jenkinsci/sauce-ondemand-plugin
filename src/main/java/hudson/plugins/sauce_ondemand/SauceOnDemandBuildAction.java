package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import hudson.model.AbstractBuild;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Presents the links to the Sauce OnDemand jobs on the build summary page.
 *
 * @author Ross Rowe
 */
public class SauceOnDemandBuildAction extends AbstractAction {

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildAction.class.getName());

    /**
     * String pattern for the URL which retrieves Sauce job details from the REST API.
     */
    private static final String JOB_DETAILS_URL = "http://saucelabs.com/rest/v1/%1$s/build/%2$s/jobs?full=true";

    /**
     * Regex pattern that is used to identify Sauce job ids which have been run as part of a Jenkins build.
     */
    public static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)(?:.job-name=(.*))?");

    private AbstractBuild<?, ?> build;
    private List<JobInformation> jobInformation;
    @Deprecated
    private String accessKey;
    @Deprecated
    private String username;

    @DataBoundConstructor
    public SauceOnDemandBuildAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public boolean hasSauceOnDemandResults() {
        if (jobInformation == null) {
            //hasn't been initialized by build action yet, return false
            return false;
        }
        return !getJobs().isEmpty();
    }

    public List<JobInformation> getJobs() {
        if (jobInformation == null) {
            try {
                jobInformation = new ArrayList<JobInformation>();
                jobInformation.addAll(retrieveJobIdsFromSauce(getSauceREST(), build).values());
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }

        return jobInformation;
    }

    @Override
    protected SauceCredentials getCredentials() {
        return SauceCredentials.getCredentials(getBuild());
    }

    /**
     * Invokes the Sauce REST API to retrieve the details for the jobs the user has access to.  Iterates over the jobs
     * and attempts to find the job that has a 'build' field matching the build key/number.
     * @return List of processed job information
     * @throws JSONException Not json returned properly
     */
    public static LinkedHashMap<String, JobInformation> retrieveJobIdsFromSauce(SauceREST sauceREST, AbstractBuild build) throws JSONException {
        SauceCredentials credentials = SauceCredentials.getCredentials(build);

        //invoke Sauce Rest API to find plan results with those values
        LinkedHashMap<String, JobInformation> jobInformation = new LinkedHashMap<String, JobInformation>();

        String buildNumber = SauceOnDemandBuildWrapper.sanitiseBuildNumber(SauceEnvironmentUtil.getBuildName(build));
        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);
        String jsonResponse = sauceREST.getBuildFullJobs(buildNumber, 5000);
        JSONObject job = new JSONObject(jsonResponse);
        JSONArray jobResults = job.getJSONArray("jobs");
        if (jobResults == null) {
            logger.log(Level.WARNING, "Unable to find job data for " + buildNumber);

        } else {
            //the list of results retrieved from the Sauce REST API is last-first, so reverse the list
            for (int i = jobResults.length() - 1; i > 0; i--) {
                //check custom data to find job that was for build
                JSONObject jobData = jobResults.getJSONObject(i);
                String jobId = jobData.getString("id");
                JobInformation information = new JenkinsJobInformation(jobId, credentials.getHMAC(jobId));
                information.populateFromJson(jobData);
                jobInformation.put(information.getJobId(), information);
            }
        }
        return jobInformation;
    }

    protected JenkinsSauceREST getSauceREST() {
        SauceCredentials creds = SauceCredentials.getCredentials(this.getBuild().getProject());
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

    public void setJobs(List<JobInformation> jobs) {
        this.jobInformation = jobs;
    }
}
