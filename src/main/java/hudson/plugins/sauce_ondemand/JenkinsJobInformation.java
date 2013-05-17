package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;

/**
 * @author Ross Rowe
 */
public class JenkinsJobInformation extends JobInformation {
    public JenkinsJobInformation(String jobId, String hmac) {
        super(jobId, hmac);
    }

    public String getResult() {
       return getStatus() == "true" ? "OK" : "FAILURE";

    }
}
