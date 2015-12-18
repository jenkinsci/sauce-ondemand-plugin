package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.SauceREST;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

/**
 * Jenkins-specific subclass which will open a URL connection using {@link hudson.ProxyConfiguration}.
 *
 * @author Ross Rowe
 */
public class JenkinsSauceREST extends SauceREST {
    public static String pluginVersion = "";
    public JenkinsSauceREST(String username, String accessKey) {
        super(username, accessKey);
    }

    public static String getPluginVersion() {
        return pluginVersion;
    }

    public static void setPluginVersion(String pluginVersion) {
        JenkinsSauceREST.pluginVersion = pluginVersion;
    }

    @Override
    protected String getUserAgent() {
        return super.getUserAgent() + " " +
            "Jenkins/" + Jenkins.VERSION.toString() + " " +
            "JenkinsSauceOnDemand/" + getPluginVersion();
    }

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection hc = (HttpURLConnection) ProxyConfiguration.open(url);
        return hc;
    }

    /* Just until saucerest api gets updated
    FIXME
     */
    public String getBuildJobs(String build, boolean full) {
        URL restEndpoint = this.buildURL("v1/" + this.username + "/build/" + build + "/jobs" + (full ? "?full=1" : ""));
        return retrieveResults(restEndpoint);
    }

}
