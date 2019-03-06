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
    private String server;

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceTestResultsById.class.getName());

    public SauceTestResultsById(String id, SauceCredentials credentials) {
        this(id, credentials, "https://saucelabs.com/");
    }

    public SauceTestResultsById(String id, SauceCredentials credentials, String restEndpoint) {
        this.id = id;
        this.credentials = credentials;
        this.job = new JenkinsJobInformation(id, credentials.getHMAC(id));
        this.server = restEndpoint.replace("https://","https://app.");
        JenkinsSauceREST sauceREST = new JenkinsSauceREST(credentials.getUsername(), credentials.getPassword().getPlainText());
        if (restEndpoint != null) {
            sauceREST.setServer(restEndpoint);
        }

        try {
            String jsonResponse = sauceREST.getJobInfo(id);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            this.job.populateFromJson(jsonObject);
        } catch (JSONException e) { // fallback for EU
            try {
                this.server = "https://app.eu-central-1.saucelabs.com/";
                sauceREST.setServer(this.server);
                String jsonResponse = sauceREST.getJobInfo(id);
                JSONObject jsonObject = new JSONObject(jsonResponse);
                this.job.populateFromJson(jsonObject);
            } catch (JSONException realException) {
                logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", realException);
            }
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
