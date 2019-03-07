package hudson.plugins.sauce_ondemand;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

@Deprecated
public class Credentials {

    private String username;
    private String apiKey;
    private String restEndpoint;

    @DataBoundConstructor
    public Credentials(String username, String apiKey, String restEndpoint) {
        this.username = username;
        this.apiKey = apiKey;
        this.restEndpoint = restEndpoint;
    }

    public String getUsername() {
        return username;
    }
    public String getApiKey() {
        return apiKey;
    }
    public String getRestEndpoint() {
        // legacy support for older credentials without restEndpoint
        if (restEndpoint == null || restEndpoint.isEmpty()) {
            return "https://saucelabs.com/";
        }
        return restEndpoint;
    }
}
