package hudson.plugins.sauce_ondemand.credentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.sun.net.httpserver.HttpServer;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.plugins.sauce_ondemand.PluginImpl;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.util.NameValuePair;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockFolder;

/**
 * Regression tests for SECURITY-3770 / CVE-2026-70445.
 * <p>
 * The credentials form-validation endpoints of the plugin must not leak credentials IDs (or Sauce usernames)
 * to users with only Overall/Read, System-scoped credentials must not be offered to jobs, and the access key
 * check must neither be reachable via GET nor usable as a credentials oracle by unprivileged users.
 */
public class SauceCredentialsSecurityTest {

    private static final String GLOBAL_USER = "fakeuser";
    private static final String SYSTEM_ID = "sauce-system-scoped";
    private static final String SYSTEM_USER = "sysuser";

    private static final String ROOT_FILL_URL =
        "descriptorByName/hudson.plugins.sauce_ondemand.PluginImpl/fillCredentialIdItems";
    private static final String CHECK_API_KEY_URL =
        "descriptorByName/hudson.plugins.sauce_ondemand.credentials.SauceCredentials/checkApiKey";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private String globalId;
    private FreeStyleProject freestyle;
    private WorkflowJob pipeline;
    private HttpServer fakeSauce;
    private final AtomicInteger outboundRequests = new AtomicInteger();

    /** What the stand-in Sauce Labs accounts API answers. 401 is what an invalid key looks like. */
    private volatile int sauceStatus = 401;
    private volatile String sauceBody = "{\"message\":\"Unauthorized\"}";

    private SauceCredentials.DescriptorImpl descriptor() {
        return j.jenkins.getDescriptorByType(SauceCredentials.DescriptorImpl.class);
    }

    private static ACLContext as(String user) {
        return ACL.as2(User.getById(user, true).impersonate2());
    }

    @Before
    public void setUp() throws Exception {
        freestyle = j.createFreeStyleProject("freestyle");
        pipeline = j.createProject(WorkflowJob.class, "pipeline");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.ADMINISTER).everywhere().to("admin")
            .grant(Jenkins.READ, Item.READ).everywhere().to("reader")
            .grant(Jenkins.READ).everywhere().to("configurer")
            .grant(Item.READ, Item.CONFIGURE).onItems(freestyle, pipeline).to("configurer")
            .grant(Jenkins.READ).everywhere().to("user-of-credentials")
            .grant(Item.READ, CredentialsProvider.USE_ITEM).onItems(freestyle).to("user-of-credentials"));

        globalId = SauceCredentials.migrateToCredentials(GLOBAL_USER, "fakekey", null, "SauceCredentialsSecurityTest");
        SystemCredentialsProvider.getInstance().getCredentials().add(new SauceCredentials(
            CredentialsScope.SYSTEM, SYSTEM_ID, SYSTEM_USER, "syskey", "https://saucelabs.com/", "system scoped"));
        SystemCredentialsProvider.getInstance().save();

        // Stand-in for the Sauce Labs accounts API: counts requests and always answers 401, which saucerest
        // maps to SauceException.NotAuthorized (other status codes are retried or turned into RuntimeExceptions).
        fakeSauce = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeSauce.createContext("/", exchange -> {
            // The plugin asks for supported browsers from a static initialiser while it loads. That call
            // must succeed or the whole plugin fails to load, so it is answered separately and not counted.
            if (exchange.getRequestURI().getPath().contains("/info/platforms")) {
                byte[] platforms = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, platforms.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(platforms);
                }
                return;
            }
            outboundRequests.incrementAndGet();
            byte[] body = sauceBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(sauceStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        fakeSauce.start();
        System.setProperty("saucerest-java.base_api_url",
            "http://127.0.0.1:" + fakeSauce.getAddress().getPort() + "/");
    }

    @After
    public void tearDown() {
        System.clearProperty("saucerest-java.base_api_url");
        if (fakeSauce != null) {
            fakeSauce.stop(0);
        }
    }

    private String wrapperFillUrl() {
        return freestyle.getUrl()
            + "descriptorByName/hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper/fillCredentialIdItems";
    }

    private String sauceStepFillUrl() {
        return pipeline.getUrl() + "descriptorByName/com.saucelabs.jenkins.pipeline.SauceStep/fillCredentialsIdItems";
    }

    private String sauceConnectStepFillUrl() {
        return pipeline.getUrl()
            + "descriptorByName/com.saucelabs.jenkins.pipeline.SauceConnectStep/fillCredentialsIdItems";
    }

    private String[] jobFillUrls() {
        return new String[] {wrapperFillUrl(), sauceStepFillUrl(), sauceConnectStepFillUrl()};
    }

    /**
     * Requests a fill endpoint the way Jenkins itself does. Core's {@code select.js} fetches fill URLs
     * with POST and a CSRF crumb, and the endpoints are annotated {@code @POST} to match, so a GET would
     * be answered with 404 and prove nothing about permissions.
     */
    private String fillAs(String user, String relativeUrl) throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient().login(user);
        WebRequest req = new WebRequest(new URL(j.getURL(), relativeUrl), HttpMethod.POST);
        wc.addCrumb(req);
        return wc.getPage(req).getWebResponse().getContentAsString();
    }

    private static void assertLeaksNothing(String url, String body, String globalId) {
        assertThat(url, body, not(containsString(globalId)));
        assertThat(url, body, not(containsString(GLOBAL_USER)));
        assertThat(url, body, not(containsString(SYSTEM_ID)));
        assertThat(url, body, not(containsString(SYSTEM_USER)));
    }

    private static boolean contains(ListBoxModel model, String value) {
        return model.stream().anyMatch(o -> value.equals(o.value));
    }

    @Test
    public void readerCannotEnumerateCredentialsIds() throws Exception {
        assertLeaksNothing(ROOT_FILL_URL, fillAs("reader", ROOT_FILL_URL), globalId);
        for (String url : jobFillUrls()) {
            assertLeaksNothing(url, fillAs("reader", url), globalId);
        }
    }

    @Test
    public void configurerCannotEnumerateCredentialsIdsInGlobalConfiguration() throws Exception {
        assertLeaksNothing(ROOT_FILL_URL, fillAs("configurer", ROOT_FILL_URL), globalId);
    }

    @Test
    public void adminSeesCredentialsInGlobalConfiguration() throws Exception {
        assertThat(fillAs("admin", ROOT_FILL_URL), containsString(globalId));
    }

    @Test
    public void configurerSeesGlobalButNotSystemScopedCredentialsInJobs() throws Exception {
        for (String url : jobFillUrls()) {
            String body = fillAs("configurer", url);
            assertThat(url, body, containsString(globalId));
            assertThat(url, body, not(containsString(SYSTEM_ID)));
        }
    }

    @Test
    public void systemScopedCredentialsAreOnlyOfferedAtTheRoot() throws Exception {
        try (ACLContext ignored = ACL.as2(User.getById("admin", true).impersonate2())) {
            ListBoxModel root = j.jenkins.getDescriptorByType(PluginImpl.DescriptorImpl.class)
                .doFillCredentialIdItems(null, null);
            assertTrue(contains(root, globalId));
            assertTrue(contains(root, SYSTEM_ID));

            ListBoxModel job = j.jenkins.getDescriptorByType(SauceOnDemandBuildWrapper.DescriptorImpl.class)
                .doFillCredentialIdItems(freestyle, null);
            assertTrue(contains(job, globalId));
            assertFalse(contains(job, SYSTEM_ID));
        }
    }

    @Test
    public void currentValueIsPreservedForUnprivilegedUsers() throws Exception {
        try (ACLContext ignored = ACL.as2(User.getById("reader", true).impersonate2())) {
            ListBoxModel model = j.jenkins.getDescriptorByType(SauceOnDemandBuildWrapper.DescriptorImpl.class)
                .doFillCredentialIdItems(freestyle, globalId);
            assertEquals(1, model.size());
            assertEquals(globalId, model.get(0).value);
            assertThat(model.get(0).name, not(containsString(GLOBAL_USER)));
        }
    }

    private String postCheckApiKey(String user) throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient().login(user);
        WebRequest req = new WebRequest(new URL(j.getURL(), CHECK_API_KEY_URL), HttpMethod.POST);
        wc.addCrumb(req);
        List<NameValuePair> params = new ArrayList<>(req.getRequestParameters());
        params.add(new NameValuePair("value", "some-access-key"));
        params.add(new NameValuePair("username", "some-user"));
        req.setRequestParameters(params);
        return wc.getPage(req).getWebResponse().getContentAsString();
    }

    @Test
    public void fillEndpointsAreNotReachableViaGet() throws Exception {
        // Assert the endpoint answers a POST first. A 404 on GET only means the verb was refused if the
        // endpoint exists at all; if the plugin failed to load, everything 404s and this test would pass
        // while proving nothing.
        assertThat(fillAs("admin", ROOT_FILL_URL), containsString(globalId));

        JenkinsRule.WebClient wc = j.createWebClient().login("admin");
        wc.assertFails(ROOT_FILL_URL, 404);
        for (String url : jobFillUrls()) {
            wc.assertFails(url, 404);
        }
    }

    @Test
    public void checkApiKeyIsNotReachableViaGet() throws Exception {
        j.createWebClient().login("admin")
            .assertFails(CHECK_API_KEY_URL + "?value=some-access-key&username=some-user", 404);
        assertEquals(0, outboundRequests.get());
    }

    @Test
    public void checkApiKeyDoesNotContactSauceLabsForUnprivilegedUsers() throws Exception {
        String body = postCheckApiKey("reader");
        assertThat(body, not(containsString("error")));
        assertEquals(0, outboundRequests.get());
    }

    @Test
    public void checkApiKeyContactsSauceLabsForAdministrators() throws Exception {
        String body = postCheckApiKey("admin");
        assertThat(body, containsString("Bad username or Access key"));
        assertEquals(1, outboundRequests.get());
    }

    @Test
    public void checkApiKeyOnAnItemRequiresConfigurePermission() throws Exception {
        SauceCredentials.DescriptorImpl d = descriptor();

        try (ACLContext ignored = as("reader")) {
            assertEquals(FormValidation.Kind.OK, d.doCheckApiKey(freestyle, "a-key", "a-user", null).kind);
        }
        assertEquals("a user who cannot configure the job gets no verdict and no outbound call",
                0, outboundRequests.get());

        try (ACLContext ignored = as("configurer")) {
            FormValidation v = d.doCheckApiKey(freestyle, "a-key", "a-user", null);
            assertEquals(FormValidation.Kind.ERROR, v.kind);
        }
        assertEquals("a user who can configure the job does get a verdict", 1, outboundRequests.get());
    }

    @Test
    public void checkApiKeySkipsBlankInput() throws Exception {
        SauceCredentials.DescriptorImpl d = descriptor();
        try (ACLContext ignored = as("admin")) {
            assertEquals(FormValidation.Kind.OK, d.doCheckApiKey(null, "a-key", "   ", null).kind);
            assertEquals(FormValidation.Kind.OK, d.doCheckApiKey(null, "", "a-user", null).kind);
            assertEquals(FormValidation.Kind.OK, d.doCheckApiKey(null, null, null, null).kind);
        }
        assertEquals("blank input must not reach Sauce Labs", 0, outboundRequests.get());
    }

    @Test
    public void checkApiKeyRejectsAnUnknownDataCenter() throws Exception {
        try (ACLContext ignored = as("admin")) {
            FormValidation v = descriptor().doCheckApiKey(null, "a-key", "a-user", "NO_SUCH_DATA_CENTER");
            assertEquals(FormValidation.Kind.ERROR, v.kind);
            assertThat(v.getMessage(), containsString("Unknown data center"));
        }
        assertEquals("an unknown data center must not reach Sauce Labs", 0, outboundRequests.get());
    }

    @Test
    public void checkApiKeyAcceptsAnExplicitDataCenter() throws Exception {
        try (ACLContext ignored = as("admin")) {
            // exercises the non-null side of the data centre default
            assertEquals(FormValidation.Kind.ERROR,
                    descriptor().doCheckApiKey(null, "a-key", "a-user", "US_WEST").kind);
        }
        assertEquals(1, outboundRequests.get());
    }

    @Test
    public void checkApiKeyReportsBadCredentialsWhenSauceReturnsAnEmptyUsername() throws Exception {
        sauceStatus = 200;
        sauceBody = "{\"username\":\"\"}";
        try (ACLContext ignored = as("admin")) {
            FormValidation v = descriptor().doCheckApiKey(null, "a-key", "a-user", null);
            assertEquals(FormValidation.Kind.ERROR, v.kind);
            assertThat(v.getMessage(), containsString("Bad username or Access key"));
        }
        assertEquals(1, outboundRequests.get());
    }

    @Test
    public void checkApiKeyAcceptsCredentialsSauceRecognises() throws Exception {
        sauceStatus = 200;
        sauceBody = "{\"username\":\"a-user\"}";
        try (ACLContext ignored = as("admin")) {
            assertEquals(FormValidation.Kind.OK, descriptor().doCheckApiKey(null, "a-key", "a-user", null).kind);
        }
        assertEquals(1, outboundRequests.get());
    }

    @Test
    public void credentialsUseItemPermissionAloneIsEnoughToPickACredential() throws Exception {
        // Item/ExtendedRead is not the only way in: Credentials/UseItem also qualifies, and that user
        // deliberately has no Item/Configure, which would have implied ExtendedRead.
        try (ACLContext ignored = as("user-of-credentials")) {
            assertFalse(freestyle.hasPermission(Item.EXTENDED_READ));
            assertTrue(freestyle.hasPermission(CredentialsProvider.USE_ITEM));

            ListBoxModel model = SauceCredentials.fillCredentialsIdItems(freestyle, null);
            assertTrue(contains(model, globalId));
            assertFalse("System-scoped credentials still never reach a job", contains(model, SYSTEM_ID));
        }
    }

    @Test
    public void credentialsCanBeListedForAnItemThatIsNotABuildableTask() throws Exception {
        // A folder is an Item but not a Queue.Task, so there is no build authentication to borrow and the
        // lookup falls back to SYSTEM in the folder's own context.
        MockFolder folder = j.createFolder("a-folder");
        try (ACLContext ignored = as("admin")) {
            ListBoxModel model = SauceCredentials.fillCredentialsIdItems(folder, null);
            assertTrue(contains(model, globalId));
            assertFalse("System-scoped credentials must not reach a folder either",
                    contains(model, SYSTEM_ID));
        }
    }
}
