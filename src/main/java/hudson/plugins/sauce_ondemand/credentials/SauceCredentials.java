package hudson.plugins.sauce_ondemand.credentials;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.SauceShareableLink;
import com.saucelabs.saucerest.api.AccountsEndpoint;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.plugins.sauce_ondemand.BuildUtils;
import hudson.plugins.sauce_ondemand.JenkinsSauceREST;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class SauceCredentials extends BaseStandardCredentials implements StandardUsernamePasswordCredentials {
    /**
     * The username.
     */
    protected final String username;

    /**
     * The Password/apikey
     */
    protected final Secret apiKey;

    /**
     * The data center endpoint
     */
    protected final String restEndpoint;

    protected ShortLivedConfig shortLivedConfig;

    @DataBoundConstructor
    public SauceCredentials(@CheckForNull CredentialsScope scope, @CheckForNull String id,
            @NonNull String username, @NonNull String apiKey, @NonNull String restEndpoint, @CheckForNull String description) {
            super(scope, id, description);
            this.apiKey = Secret.fromString(apiKey);
            this.username = username;
            this.restEndpoint = restEndpoint;
    }

    public ShortLivedConfig getShortLivedConfig() {
        return shortLivedConfig;
    }

    @DataBoundSetter
    public void setShortLivedConfig(ShortLivedConfig shortLivedConfig) {
        this.shortLivedConfig = shortLivedConfig;
    }

    public static SauceCredentials getCredentials(AbstractProject project) {
        if (project == null) { return null; }

        if (!(project instanceof BuildableItemWithBuildWrappers)) {
            return getCredentials((AbstractProject) project.getParent());
        }
        BuildableItemWithBuildWrappers p = (BuildableItemWithBuildWrappers) project;
        SauceOnDemandBuildWrapper bw = p.getBuildWrappersList().get(SauceOnDemandBuildWrapper.class);
        if (bw == null) { return null; }
        String credentialsId = bw.getCredentialId();
        return getCredentialsById((Item) p, credentialsId);
    }

    public static SauceCredentials getCredentials(AbstractBuild build) {
        return getCredentials(build.getProject());
    }

    @NonNull
    public Secret getPassword() {
        if (this.getShortLivedConfig() != null) {
            try {
                Date d = new Date();
                Date expires = new Date(
                        System.currentTimeMillis() +
                        (long) this.getShortLivedConfig().getTime() * 1000 /* to millis */ * 60 /* to minutes */
                        );

                String token = JWT.create()
                    .withIssuer("Jenkins/" + Jenkins.VERSION + " JenkinsSauceOnDemand/" + BuildUtils.getCurrentVersion())
                    .withExpiresAt(expires)
                    .withIssuedAt(d)
                    .sign(Algorithm.HMAC256(this.apiKey.getPlainText()));
                return Secret.fromString(token);
            } catch (JWTCreationException e){
                //Invalid Signing configuration / Couldn't convert Claims.
                e.printStackTrace();
            }
        }
        return this.getApiKey();
    }

    @NonNull
    public Secret getApiKey() {
        return this.apiKey;
    }

    @NonNull
    public String getRestEndpoint() {
        // legacy support for older credentials without restEndpoint set
        if (this.restEndpoint == null || this.restEndpoint.isEmpty()) {
            return "https://saucelabs.com/";
        }
        return this.restEndpoint;
    }

    @NonNull
    public String getRestEndpointName() {
        if (this.restEndpoint == null) {
            return "US_WEST";
        }

        switch (this.restEndpoint) {
            case "https://eu-central-1.saucelabs.com/":
                return "EU_CENTRAL";
            case "https://us-east-1.saucelabs.com/":
                return "US_EAST";
            default:
            case "https://saucelabs.com/":
                return "US_WEST";
        }
    }

    @NonNull
    public String getUsername() { return this.username; }

    @Override
    public String toString() {
        return "SauceCredentials{" +
            "apiKey=" + apiKey +
            ", username='" + username + '\'' +
            ", restEndpoint='" + restEndpoint + '\'' +
            '}';
    }

    public static SauceCredentials getSauceCredentials(AbstractBuild build, SauceOnDemandBuildWrapper wrapper) {
        String credentialId = wrapper.getCredentialId();
        return getCredentialsById(build.getProject(), credentialId);
    }

    public JenkinsSauceREST getSauceREST() {
        DataCenter dc = DataCenter.fromString(getRestEndpointName());
        JenkinsSauceREST sauceREST = new JenkinsSauceREST(getUsername(), getPassword().getPlainText(), dc);
        return sauceREST;
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor
    {
        @Override
        public String getDisplayName() {
            return "Sauce Labs";
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckApiKey(@QueryParameter String value, @QueryParameter String username, @QueryParameter String dataCenter) {
            if (dataCenter == null) {
                dataCenter = "US_WEST";
            }

            DataCenter dc = DataCenter.fromString(dataCenter);

            JenkinsSauceREST rest = new JenkinsSauceREST(username, value, dc);
            AccountsEndpoint users = rest.getAccountsEndpoint();
            // If unauthorized getUser returns an empty string.
            try {
                if (users.getUser("me").username.equals("")) {
                    return FormValidation.error("Bad username or Access key");
                }
            } catch (IOException|com.saucelabs.saucerest.SauceException.NotAuthorized e) {
                return FormValidation.error("Bad username or Access key");
            }
            return FormValidation.ok();
        }

        @Override
        public String getIconClassName() {
            return "icon-sauce-ondemand-credential";
        }
    }

    public final static DomainRequirement DOMAIN_REQUIREMENT = new HostnamePortRequirement("saucelabs.com", 80);

    public static String migrateToCredentials(String username, String accessKey, String restEndpoint, String migratedFrom) throws InterruptedException, IOException {
        final List<SauceCredentials> credentialsForDomain = SauceCredentials.all((Item) null);
        final StandardUsernameCredentials existingCredentials = CredentialsMatchers.firstOrNull(
            credentialsForDomain,
            CredentialsMatchers.withUsername(username)
        );

        final String credentialId;
        if (existingCredentials == null) {
            String createdCredentialId = UUID.randomUUID().toString();

            final StandardUsernameCredentials credentialsToCreate;
            if (accessKey != null && !accessKey.isEmpty()) {
                credentialsToCreate = new SauceCredentials(
                    CredentialsScope.GLOBAL,
                    createdCredentialId,
                    username,
                    accessKey,
                    restEndpoint,
                    "migrated from " + migratedFrom
                );
            } else {
                throw new InterruptedException("Did not find password");
            }

            final SystemCredentialsProvider credentialsProvider = SystemCredentialsProvider.getInstance();
            final Map<Domain, List<Credentials>> credentialsMap = credentialsProvider.getDomainCredentialsMap();

            final Domain domain = Domain.global();
            if (credentialsMap.get(domain) == null) {
                credentialsMap.put(domain, Collections.EMPTY_LIST);
            }
            credentialsMap.get(domain).add(credentialsToCreate);

            credentialsProvider.setDomainCredentialsMap(credentialsMap);
            credentialsProvider.save();

            credentialId = createdCredentialId;
        } else {
            credentialId = existingCredentials.getId();
        }

        return credentialId;
    }

    public static List<SauceCredentials> all(ItemGroup context) {
        return CredentialsProvider.lookupCredentials(
            SauceCredentials.class,
            context,
            ACL.SYSTEM,
            SauceCredentials.DOMAIN_REQUIREMENT
        );
    }

    public static List<SauceCredentials> all(Item context) {
        return CredentialsProvider.lookupCredentials(
            SauceCredentials.class,
            context,
            ACL.SYSTEM,
            SauceCredentials.DOMAIN_REQUIREMENT
        );
    }

    public static SauceCredentials getCredentialsById(Item context, String id) {
        return CredentialsMatchers.firstOrNull(
            SauceCredentials.all((Item) context),
            CredentialsMatchers.withId(id)
        );
    }


    /**
     *
     */
    private static final String HMAC_KEY = "HMACMD5";
    /**
     * Format for the date component of the HMAC.
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd-HH";


    /**
     * Creates a HMAC token which is used as part of the Javascript inclusion that embeds the Sauce results
     *
     * @param jobId     the Sauce job id
     * @return the HMAC token
     *
     */
    public String getHMAC(String jobId) {
        return SauceShareableLink.getJobAuthDigest(username, getPassword().getPlainText(), jobId);
    }

    public static final class ShortLivedConfig extends AbstractDescribableImpl<ShortLivedConfig> implements Serializable {
        protected final Integer time;

        @DataBoundConstructor
        public ShortLivedConfig(Integer time) {
            this.time = time;
        }

        public Integer getTime() {
            return time;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<ShortLivedConfig> {
            @Override
            public String getDisplayName() { return ""; }
        }

    }
}