package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import hudson.model.AbstractBuild;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.tasks.junit.CaseResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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

    private transient final SauceOnDemandBuildWrapper.SauceOnDemandLogParser logParser;

    private AbstractBuild<?, ?> build;
    private List<JobInformation> jobInformation;
    @Deprecated
    private String accessKey;
    @Deprecated
    private String username;

    @DataBoundConstructor
    public SauceOnDemandBuildAction(AbstractBuild<?, ?> build, SauceOnDemandBuildWrapper.SauceOnDemandLogParser logParser) {
        this.build = build;
        this.logParser = logParser;
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
                jobInformation.addAll(retrieveJobIdsFromSauce());
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
    public List<JobInformation> retrieveJobIdsFromSauce() throws JSONException {
        SauceCredentials credentials = SauceCredentials.getCredentials(build);

        //invoke Sauce Rest API to find plan results with those values
        List<JobInformation> jobInformation = new ArrayList<JobInformation>();

        JenkinsSauceREST sauceREST = getSauceREST();
        String buildNumber = SauceOnDemandBuildWrapper.sanitiseBuildNumber(SauceEnvironmentUtil.getBuildName(build));
        logger.fine("Performing Sauce REST retrieve results for " + buildNumber);
        String jsonResponse = sauceREST.getBuildFullJobs(buildNumber);
        JSONObject job = new JSONObject(jsonResponse);
        JSONArray jobResults = job.getJSONArray("jobs");
        if (jobResults == null) {
            logger.log(Level.WARNING, "Unable to find job data for " + buildNumber);

        } else {
            for (int i = 0; i < jobResults.length(); i++) {
                //check custom data to find job that was for build
                JSONObject jobData = jobResults.getJSONObject(i);
                String jobId = jobData.getString("id");
                JobInformation information = new JenkinsJobInformation(jobId, credentials.getHMAC(jobId));
                information.populateFromJson(jobData);
                jobInformation.add(information);
            }
            //the list of results retrieved from the Sauce REST API is last-first, so reverse the list
            Collections.reverse(jobInformation);
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

    protected JobInformation jobInformationForBuild(String jobId) {
        for (JobInformation jobInfo : getJobs()) {
            if (jobId.equals(jobInfo.getJobId())) {
                return jobInfo;
            }
        }
        return null;
    }

    public SauceOnDemandBuildWrapper.SauceOnDemandLogParser getLogParser() {
        return logParser;
    }

    /**
     * Processes the log output, and for lines which are in the valid log format, add a new {@link JobInformation}
     * instance to the {@link #jobInformation} list.
     *
     * @param caseResult test results being processed, can be null
     * @param output     lines of output to be processed, not null
     */
    public void processSessionIds(CaseResult caseResult, String... output) {
        SauceCredentials credentials = SauceCredentials.getCredentials(build);

        logger.log(Level.FINE, caseResult == null ? "Parsing Sauce Session ids in stdout" : "Parsing Sauce Session ids in test results");
        SauceREST sauceREST = getSauceREST();

        for (String text : output) {
            if (text == null) continue;
            Matcher m = SESSION_ID_PATTERN.matcher(text);
            while (m.find()) {
                String jobId = m.group(1);
                String jobName = null;
                if (m.groupCount() == 2) {
                    jobName = m.group(2);
                }
                JobInformation jobInfo = jobInformationForBuild(jobId);
                if (jobInfo != null) {
                    //we already have the job information stored, move to the next match
                    continue;
                }
                try {
                    jobInfo = new JenkinsJobInformation(jobId, credentials.getHMAC(jobId));
                    //retrieve data from session id to see if build number and/or job name has been stored
                    String jsonResponse = sauceREST.getJobInfo(jobId);
                    if (!jsonResponse.equals("")) {
                        JSONObject job = new JSONObject(jsonResponse);
                        jobInfo.populateFromJson(job);
                    }
                    if (!jobInfo.hasJobName() && jobName != null) {
                        jobInfo.setName(jobName);
                    }
                    jobInformation.add(jobInfo);
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
                }

            }
        }
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


}
