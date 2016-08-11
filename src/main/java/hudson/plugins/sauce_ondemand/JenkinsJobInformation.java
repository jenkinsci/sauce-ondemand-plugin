package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * @author Ross Rowe
 */
public class JenkinsJobInformation extends JobInformation {
    public JenkinsJobInformation(String jobId, String hmac) {
        super(jobId, hmac);
    }

    public String getResult() {
       return Objects.equals("true", getStatus()) ? "OK" : "FAILURE";
    }

    @Override
    public String getLogUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(super.getLogUrl());
            uriBuilder.addParameter("auth", this.getHmac());
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return super.getLogUrl();
    }

    @Override
    public String getVideoUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(super.getVideoUrl());
            uriBuilder.addParameter("auth", this.getHmac());
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return super.getVideoUrl();
    }
}
