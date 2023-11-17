package hudson.plugins.sauce_ondemand;

import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.HttpClientConfig;
import hudson.ProxyConfiguration;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Objects;
import jenkins.model.Jenkins;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Jenkins-specific subclass which will open a URL connection using {@link
 * hudson.ProxyConfiguration}.
 *
 * @author Ross Rowe
 */
public class JenkinsSauceREST extends SauceREST {

  protected static final String userAgent =
      "Jenkins/" + Jenkins.VERSION + " " + "JenkinsSauceOnDemand/" + BuildUtils.getCurrentVersion();
  private String server = getSauceRestUrlFromEnv();

  public JenkinsSauceREST(String username, String accessKey, DataCenter dataCenter) {
    super(username, accessKey, dataCenter, getJenkinsPluginHttpConfig(dataCenter));
    if (server == null) {
      server = dataCenter.server();
    }
  }

  private static String getSauceRestUrlFromEnv() {
    String srUrl = System.getenv("SAUCE_REST_ENDPOINT");
    if (srUrl == null) {
      return System.getProperty("saucerest-java.base_url");
    }
    return srUrl;
  }

  private static HttpClientConfig getJenkinsPluginHttpConfig(DataCenter dataCenter) {
    String server = getSauceRestUrlFromEnv();
    if (server == null) {
      server = dataCenter.server();
    }
    Proxy proxy = null;
    Authenticator auth = Authenticator.NONE;
    ProxyConfiguration pc = Jenkins.get().getProxy();

    if (pc != null) {
      String host = Objects.requireNonNull(buildURL(server)).getHost();
      proxy = pc.createProxy(host);

      if (pc.getUserName() != null
          && !pc.getUserName().isEmpty()
          && pc.getSecretPassword() != null
          && !pc.getSecretPassword().getPlainText().isEmpty()) {
        auth = new ProxyAuthenticator(pc.getUserName(), pc.getSecretPassword().getPlainText());
      }
    }
    UserAgentInterceptor ua = new UserAgentInterceptor(userAgent);

    return HttpClientConfig.defaultConfig().proxy(proxy).authenticator(auth).interceptor(ua);
  }

  protected static URL buildURL(String server) {
    try {
      return new URL(new URL(server), "/rest/v1/");
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * In the publisher step we need to manually set this as the env var will not be correctly set
   * from earlier and will be the default
   */
  public void setServer(String server) {
    if (server != null && !server.isEmpty()) {
      this.server = server;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof JenkinsSauceREST)) {
      return super.equals(obj);
    }
    JenkinsSauceREST sauceobj = (JenkinsSauceREST) obj;
    return Objects.equals(sauceobj.username, this.username)
        && Objects.equals(sauceobj.accessKey, this.accessKey)
        && Objects.equals(sauceobj.server, this.server);
  }

  @Override
  public int hashCode() {
    return Objects.hash(server, username, accessKey);
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
    return response.request().newBuilder().header("Proxy-Authorization", this.credentials).build();
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
    Request requestWithUserAgent =
        originalRequest.newBuilder().header("User-Agent", userAgent).build();
    return chain.proceed(requestWithUserAgent);
  }
}
