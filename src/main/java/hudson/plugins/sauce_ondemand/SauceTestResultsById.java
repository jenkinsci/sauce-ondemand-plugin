package hudson.plugins.sauce_ondemand;

import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;

import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SauceTestResultsById {
    private final String id;
    private final SauceCredentials credentials;
    private final JenkinsJobInformation job;
    private final String server;

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceTestResultsById.class.getName());

    public SauceTestResultsById(String id, SauceCredentials credentials) {
        this.id = id;
        this.credentials = credentials;
        this.job = new JenkinsJobInformation(id, credentials.getHMAC(id));
        this.server = credentials.getRestEndpoint().replace("https://","https://app.");
        JenkinsSauceREST sauceREST = new JenkinsSauceREST(credentials.getUsername(), credentials.getPassword().getPlainText());

        try {
            String jsonResponse = sauceREST.getJobInfo(id);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            this.job.populateFromJson(jsonObject);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
        }
    }

    public String getId() {
        return id;
    }

    public String getAuth() throws IOException {
        return credentials.getHMAC(this.id);
    }

    public String getServer() {
        return server;
    }

    public String getName() {
        return job.getName();
    }

    public boolean hasFailureMessage() {
        return job.hasFailureMessage();
    }

    public String getFailureMessage() {
        return job.getFailureMessage();
    }

    public String getJobId() {
        return job.getJobId();

    }
}
