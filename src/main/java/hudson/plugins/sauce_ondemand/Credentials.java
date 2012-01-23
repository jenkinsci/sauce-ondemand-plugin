package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Ross Rowe
 */
public class Credentials {

    private String username;
    private String apiKey;

    @DataBoundConstructor
    public Credentials(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
