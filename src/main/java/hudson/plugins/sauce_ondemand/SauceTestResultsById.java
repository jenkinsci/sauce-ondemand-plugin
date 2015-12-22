package hudson.plugins.sauce_ondemand;

import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;

import java.io.IOException;

/**
 *
 */
public class SauceTestResultsById {
    private final String id;
    private final SauceCredentials credentials;

    public SauceTestResultsById(String id, SauceCredentials credentials) {
        this.id = id;
        this.credentials = credentials;
    }

    public String getId() {
        return id;
    }

    public String getAuth() throws IOException {
        return credentials.getHMAC(this.id);
    }

}
