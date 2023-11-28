package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.model.jobs.Job;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/** */
public class SauceTestResultsById {
  /** Logger instance. */
  private static final Logger logger = Logger.getLogger(SauceTestResultsById.class.getName());

  private final String id;
  private final SauceCredentials credentials;
  private final JenkinsJobInformation job;
  private final String server;

  public SauceTestResultsById(String id, SauceCredentials credentials, JenkinsSauceREST sauceREST) {
    this.id = id;
    this.credentials = credentials;
    this.job = new JenkinsJobInformation(id, credentials.getHMAC(id));
    this.server = credentials.getRestEndpoint().replace("https://", "https://app.");
    JobsEndpoint jobs = sauceREST.getJobsEndpoint();
    try {
      Job job = jobs.getJobDetails(id);
      this.job.populate(job);
    } catch (JSONException | IOException e) {
      logger.log(Level.WARNING, "Unable to retrieve Job data from Sauce Labs", e);
    }
  }

  public SauceTestResultsById(String id, SauceCredentials credentials) {
    this(
        id,
        credentials,
        new JenkinsSauceREST(
            credentials.getUsername(),
            credentials.getPassword().getPlainText(),
            DataCenter.fromString(credentials.getRestEndpointName())));
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
