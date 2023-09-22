package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.api.AccountsEndpoint;
import com.saucelabs.saucerest.api.BuildsEndpoint;
import com.saucelabs.saucerest.api.InsightsEndpoint;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.api.PlatformEndpoint;
import com.saucelabs.saucerest.api.PerformanceEndpoint;
import com.saucelabs.saucerest.api.RealDevicesEndpoint;
import com.saucelabs.saucerest.api.SauceConnectEndpoint;
import com.saucelabs.saucerest.api.StorageEndpoint;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.OkHttpClient;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Route;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * Jenkins-specific subclass which will open a URL connection using {@link hudson.ProxyConfiguration}.
 *
 * @author Ross Rowe
 */
public class JenkinsSauceREST extends SauceREST {

    private String server;
    protected String userAgent;

    // TODO: May need EDS Urls in the future
    public JenkinsSauceREST(String username, String accessKey, DataCenter dataCenter) {
        super(username, accessKey, dataCenter);

        this.userAgent = "Jenkins/" + Jenkins.VERSION + " " + "JenkinsSauceOnDemand/" + BuildUtils.getCurrentVersion();

        // Set the server address according to the data center, but allow it to be
        // overridden in the config or an env var.
        final String configBase = System.getProperty("saucerest-java.base_url");

        if (System.getenv("SAUCE_REST_ENDPOINT") != null) {
            server = System.getenv("SAUCE_REST_ENDPOINT");
        } else if (configBase != null) {
            server = configBase;
        } else {
            server = dataCenter.server();
        }
    }

    // useful for debugging
    public String getRESTURL() {
        return this.buildURL("").toString();
    }

    private OkHttpClient getHTTPClient() {
        Proxy proxy = null;
        Authenticator auth = Authenticator.NONE;

        ProxyConfiguration pc = Jenkins.getActiveInstance().proxy;
        if (pc != null) {
            proxy = pc.createProxy(this.buildURL("").getHost());

            if (pc.getUserName() != null && pc.getPassword() != null) {
                auth = new ProxyAuthenticator(pc.getUserName(), pc.getPassword());
            }
        }

        UserAgentInterceptor ua = new UserAgentInterceptor(this.userAgent);

        return new OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .proxy(proxy)
            .proxyAuthenticator(auth)
            .addInterceptor(ua)
            .build();
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

    protected URL buildURL(String endpoint) {
        try {
            return new URL(new URL(this.server), "/rest/v1/" + endpoint);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JenkinsSauceREST)) {
            return super.equals(obj);
        }
        JenkinsSauceREST sauceobj = (JenkinsSauceREST) obj;
        return Objects.equals(sauceobj.username, this.username) &&
            Objects.equals(sauceobj.accessKey, this.accessKey) &&
            Objects.equals(sauceobj.server, this.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, username, accessKey);
    }

    @Override
    public JobsEndpoint getJobsEndpoint() {
        JobsEndpoint ep = new JobsEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(this.getHTTPClient());
        return ep;
    }

    @Override
    public JobsEndpoint getJobsEndpoint(DataCenter dataCenter) {
        JobsEndpoint ep = new JobsEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public JobsEndpoint getJobsEndpoint(String apiServer) {
        JobsEndpoint ep = new JobsEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public StorageEndpoint getStorageEndpoint() {
        StorageEndpoint ep = new StorageEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public StorageEndpoint getStorageEndpoint(DataCenter dataCenter) {
        StorageEndpoint ep = new StorageEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public StorageEndpoint getStorageEndpoint(String apiServer) {
        StorageEndpoint ep = new StorageEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PlatformEndpoint getPlatformEndpoint() {
        PlatformEndpoint ep = new PlatformEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PlatformEndpoint getPlatformEndpoint(DataCenter dataCenter) {
        PlatformEndpoint ep = new PlatformEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PlatformEndpoint getPlatformEndpoint(String apiServer) {
        PlatformEndpoint ep = new PlatformEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public RealDevicesEndpoint getRealDevicesEndpoint(DataCenter dataCenter) {
        RealDevicesEndpoint ep = new RealDevicesEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public RealDevicesEndpoint getRealDevicesEndpoint() {
        RealDevicesEndpoint ep = new RealDevicesEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public RealDevicesEndpoint getRealDevicesEndpoint(String apiServer) {
        RealDevicesEndpoint ep = new RealDevicesEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public SauceConnectEndpoint getSauceConnectEndpoint() {
        SauceConnectEndpoint ep = new SauceConnectEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public SauceConnectEndpoint getSauceConnectEndpoint(String apiServer) {
        SauceConnectEndpoint ep = new SauceConnectEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public SauceConnectEndpoint getSauceConnectEndpoint(DataCenter dataCenter) {
        SauceConnectEndpoint ep = new SauceConnectEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public AccountsEndpoint getAccountsEndpoint() {
        AccountsEndpoint ep = new AccountsEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public AccountsEndpoint getAccountsEndpoint(String apiServer) {
        AccountsEndpoint ep = new AccountsEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public AccountsEndpoint getAccountsEndpoint(DataCenter dataCenter) {
        AccountsEndpoint ep = new AccountsEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public BuildsEndpoint getBuildsEndpoint() {
        BuildsEndpoint ep = new BuildsEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public BuildsEndpoint getBuildsEndpoint(String apiServer) {
        BuildsEndpoint ep = new BuildsEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public BuildsEndpoint getBuildsEndpoint(DataCenter dataCenter) {
        BuildsEndpoint ep = new BuildsEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public InsightsEndpoint getInsightsEndpoint() {
        InsightsEndpoint ep = new InsightsEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public InsightsEndpoint getInsightsEndpoint(String apiServer) {
        InsightsEndpoint ep = new InsightsEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public InsightsEndpoint getInsightsEndpoint(DataCenter dataCenter) {
        InsightsEndpoint ep = new InsightsEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PerformanceEndpoint getPerformanceEndpoint() {
        PerformanceEndpoint ep = new PerformanceEndpoint(this.username, this.accessKey, this.apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PerformanceEndpoint getPerformanceEndpoint(String apiServer) {
        PerformanceEndpoint ep = new PerformanceEndpoint(this.username, this.accessKey, apiServer);
        ep.setClient(getHTTPClient());
        return ep;
    }

    @Override
    public PerformanceEndpoint getPerformanceEndpoint(DataCenter dataCenter) {
        PerformanceEndpoint ep = new PerformanceEndpoint(this.username, this.accessKey, dataCenter);
        ep.setClient(getHTTPClient());
        return ep;
    }
}


class ProxyAuthenticator implements Authenticator {
        private final String credentials;

        protected ProxyAuthenticator(String user, String pass) {
            this.credentials = Credentials.basic(user, pass);
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            if (response.request().header("Proxy-Authorization") != null) {
                // Authentication has already been attempted
                return null;
            }
            return response.request().newBuilder()
                .header("Proxy-Authorization", this.credentials)
                .build();
        }
}

class UserAgentInterceptor implements Interceptor {

    private final String userAgent;

    public UserAgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build();
        return chain.proceed(requestWithUserAgent);
    }
}