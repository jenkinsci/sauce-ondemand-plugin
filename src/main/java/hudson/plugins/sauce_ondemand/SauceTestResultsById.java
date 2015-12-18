package hudson.plugins.sauce_ondemand;

import hudson.plugins.sauce_ondemand.credentials.impl.SauceCredentialsImpl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class SauceTestResultsById {
    private final String id;
    private final SauceCredentialsImpl credentials;

    public SauceTestResultsById(String id, SauceCredentialsImpl credentials) {
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
