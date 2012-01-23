package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Ross Rowe
 */
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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (username != null) {
            result = 31 * result + username.hashCode();
        }
        if (apiKey != null) {
            result = 31 * result + apiKey.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Credentials)) {
            return false;
        }
        Credentials credentials = (Credentials) object;
        return (username == null ? credentials.username == null : username.equals(credentials.username)) &&
                (apiKey == null ? credentials.apiKey == null : apiKey.equals(credentials.apiKey));
    }

    @Override
    public String toString() {
        return username == null ? super.toString() : username;
    }
}
