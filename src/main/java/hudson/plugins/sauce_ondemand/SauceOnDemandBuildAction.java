package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import hudson.model.AbstractBuild;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Presents the links to the Sauce OnDemand jobs on the build summary page.
 *
 * @author Ross Rowe
 */
public class SauceOnDemandBuildAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(SauceOnDemandBuildAction.class.getName());

    private static final String DATE_FORMAT = "yyyy-MM-dd-HH";

    public static final String JOB_DETAILS_URL = "http://saucelabs.com/rest/v1/%1$s/build/%2$s/jobs?full=true";
//    public static final String JOB_DETAILS_URL = "http://saucelabs.com/rest/v1/%1$s/build/SC-SC-7/jobs?full=true";

    private static final String HMAC_KEY = "HMACMD5";

    private AbstractBuild<?, ?> build;
    private List<JobInformation> jobInformation;
    private String accessKey;
    private String username;

    public SauceOnDemandBuildAction(AbstractBuild<?, ?> build, String username, String accessKey) {
        this.build = build;
        this.username = username;
        this.accessKey = accessKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getUsername() {
        return username;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public boolean hasSauceOnDemandResults() {
        return !getJobs().isEmpty();
    }

    public List<JobInformation> getJobs() {
        if (jobInformation == null) {
            try {
                jobInformation = retrieveJobIdsFromSauce();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            } catch (InvalidKeyException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
            }
        }

        return jobInformation;
    }

    /**
     * Invokes the Sauce REST API to retrieve the details for the jobs the user has access to.  Iterates over the jobs
     * and attempts to find the job that has a 'build' field matching the build key/number.
     *
     * @throws Exception
     */
    public List<JobInformation> retrieveJobIdsFromSauce() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
        //invoke Sauce Rest API to find plan results with those values
        List<JobInformation> jobInformation = new ArrayList<JobInformation>();

        SauceREST sauceREST = new JenkinsSauceREST(username, accessKey);
        String buildNumber = SauceOnDemandBuildWrapper.sanitiseBuildNumber(getBuildName());
        String jsonResponse = sauceREST.retrieveResults(new URL(String.format(JOB_DETAILS_URL, username, buildNumber)));
        JSONObject job = new JSONObject(jsonResponse);
        JSONArray jobResults = job.getJSONArray("jobs");
        if (jobResults == null) {
            logger.log(Level.WARNING, "Unable to find job data for " + buildNumber);

        } else {
            for (int i = 0; i < jobResults.length(); i++) {
                //check custom data to find job that was for build
                JSONObject jobData = jobResults.getJSONObject(i);
                String jobId = jobData.getString("id");
                JobInformation information = new JenkinsJobInformation(jobId, calcHMAC(username, accessKey, jobId));
                String status = jobData.getString("passed");
                if (status.equals("null")) {
                    status = "not set";
                }
                information.setStatus(status);
                information.setName(jobData.getString("name"));
                jobInformation.add(information);
            }
            //the list of results retrieved from the Sauce REST API is last-first, so reverse the list
            Collections.reverse(jobInformation);
        }

        return jobInformation;
    }

    private String getBuildName() {
        String displayName =  build.getFullDisplayName();
        String buildName = build.getDisplayName();
        StringBuilder builder = new StringBuilder(displayName);
        //for multi-config projects, the full display name contains the build name twice
        //detect this and replace the second occurance with the build number
        if (StringUtils.countMatches(displayName, buildName) > 1) {
            builder.replace(displayName.lastIndexOf(buildName), displayName.length(), "#" + build.getNumber());
        }
        return builder.toString();
    }

    public void doJobReport(StaplerRequest req, StaplerResponse rsp)
            throws IOException {

        ById byId = new ById(req.getParameter("jobId"));
        try {
            req.getView(byId, "index.jelly").forward(req, rsp);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    public String calcHMAC(String username, String accessKey, String jobId) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String key = username + ":" + accessKey + ":" + format.format(calendar.getTime());
        byte[] keyBytes = key.getBytes();
        SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
        Mac mac = Mac.getInstance(sks.getAlgorithm());
        mac.init(sks);
        byte[] hmacBytes = mac.doFinal(jobId.getBytes());
        byte[] hexBytes = new Hex().encode(hmacBytes);
        return new String(hexBytes, "ISO-8859-1");
    }

    public ById getById(String id) {
        return new ById(id);
    }

    /**
     *
     */
    public class ById {
        public final String id;

        public ById(String id) {
            this.id = id;
        }

        public String getAuth() throws IOException {
            try {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String key = PluginImpl.get().getUsername() + ":" + PluginImpl.get().getApiKey() + ":" + format.format(calendar.getTime());
                byte[] keyBytes = key.getBytes();
                SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
                Mac mac = Mac.getInstance(sks.getAlgorithm());
                mac.init(sks);
                byte[] hmacBytes = mac.doFinal(id.getBytes());
                byte[] hexBytes = new Hex().encode(hmacBytes);
                return new String(hexBytes, "ISO-8859-1");


            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (InvalidKeyException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (UnsupportedEncodingException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            }

        }

    }
}
