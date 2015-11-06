package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.SauceREST;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Jenkins-specific subclass which will open a URL connection using {@link hudson.ProxyConfiguration}.
 *
 * @author Ross Rowe
 */
public class JenkinsSauceREST extends SauceREST {
    public JenkinsSauceREST(String username, String accessKey) {
        super(username, accessKey);
    }


    @Override
    protected String getUserAgent() {
        return super.getUserAgent() + " " +
            "Jenkins/" + Jenkins.VERSION.toString() + " " +
            "JenkinsSauceOnDemand/" + PluginImpl.get().getWrapper().getVersion();
    }

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection hc = (HttpURLConnection) ProxyConfiguration.open(url);
        return hc;
    }
}
