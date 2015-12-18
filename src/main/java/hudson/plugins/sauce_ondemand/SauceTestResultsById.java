package hudson.plugins.sauce_ondemand;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class SauceTestResultsById {
    private final String id;
    private final String username;
    private final String apiKey;

    public SauceTestResultsById(String id, String username, String apiKey) {
        this.id = id;
        this.username = username;
        this.apiKey = apiKey;
    }

    public String getId() {
        return id;
    }

    public String getAuth() throws IOException {
        try {
            return PluginImpl.calcHMAC(this.username, this.apiKey, this.id);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Could not generate Sauce-OnDemand access code", e);
        } catch (InvalidKeyException e) {
            throw new IOException("Could not generate Sauce-OnDemand access code", e);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Could not generate Sauce-OnDemand access code", e);
        }

    }

}
