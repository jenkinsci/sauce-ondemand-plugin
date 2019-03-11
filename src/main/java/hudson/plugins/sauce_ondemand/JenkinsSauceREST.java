package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.SauceREST;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Objects;

/**
 * Jenkins-specific subclass which will open a URL connection using {@link hudson.ProxyConfiguration}.
 *
 * @author Ross Rowe
 */
public class JenkinsSauceREST extends SauceREST {

    // TODO: May need EDS Urls in the future
    private static final String BASE_URL;
    static {
        if (System.getenv("SAUCE_REST_ENDPOINT") != null) {
            BASE_URL = System.getenv("SAUCE_REST_ENDPOINT");
        } else {
            BASE_URL = System.getProperty("saucerest-java.base_url", "https://saucelabs.com/");
        }
    }
    private String server = BASE_URL;

    static {
        SauceREST.setExtraUserAgent("Jenkins/" + Jenkins.VERSION + " " +
            "JenkinsSauceOnDemand/" + BuildUtils.getCurrentVersion());
    }
    public JenkinsSauceREST(String username, String accessKey) {
        super(username, accessKey);
    }

    // useful for debugging
    public String getRESTURL() {
        return this.buildURL("").toString();
    }

    /**
     * In the publisher step we need to manually set this as the env var
     * will not be correctly set from earlier and will be the default
     */
    public void setServer(String server) {
        if (server != null && !server.isEmpty()) {
            this.server = server;
        }
    }

    @Override
    protected URL buildURL(String endpoint) {
        try {
            return new URL(new URL(this.server), "/rest/" + endpoint);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection hc = (HttpURLConnection) ProxyConfiguration.open(url);
        return hc;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JenkinsSauceREST)) {
            return super.equals(obj);
        }
        JenkinsSauceREST sauceobj = (JenkinsSauceREST) obj;
        return Objects.equals(sauceobj.username, this.username) &&
            Objects.equals(sauceobj.accessKey, this.accessKey) &&
            Objects.equals(sauceobj.server, this.server) &&
            Objects.equals(sauceobj.BASE_URL, this.BASE_URL);
    }
}
