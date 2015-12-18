package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

@Deprecated
public class Credentials implements Serializable {

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
    public String getApiKey() {
        return apiKey;
    }

}
