package hudson.plugins.sauce_ondemand.credentials;

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
import com.google.common.base.Strings;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.SecurityUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.plugins.sauce_ondemand.JenkinsSauceREST;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SauceCredentials extends BaseStandardCredentials implements StandardUsernamePasswordCredentials {
    /**
     * The username.
     */
    protected final String username;

    /**
     * The Password/apikey
     */
    protected final Secret apiKey;

    @DataBoundConstructor
    public SauceCredentials(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                            @NonNull String username, @NonNull String apiKey, @CheckForNull String description) {
        super(scope, id, description);
        this.apiKey = Secret.fromString(apiKey);
        this.username = username;
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
        return this.getApiKey();
    }

    @NonNull
    public Secret getApiKey() { return this.apiKey; }

    @NonNull
    public String getUsername() { return this.username; }

    @Override
    public String toString() {
        return "SauceCredentials{" +
            "apiKey=" + apiKey +
            ", username='" + username + '\'' +
            '}';
    }

    public static SauceCredentials getSauceCredentials(AbstractBuild build, SauceOnDemandBuildWrapper wrapper) {
        String credentialId = wrapper.getCredentialId();
        return getCredentialsById(build.getProject(), credentialId);
    }

    public JenkinsSauceREST getSauceREST() {
        return new JenkinsSauceREST(getUsername(), getApiKey().getPlainText());
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor
    {
        @Override
        public String getDisplayName() {
            return "Sauce Labs";
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckApiKey(@QueryParameter String value, @QueryParameter String username) {
            return doValidate(username, value);
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doValidate(@QueryParameter String username, @QueryParameter String apiKey) {
            if (actualValidate(username, apiKey))
                return FormValidation.ok();
            return FormValidation.error("Bad username or Access key");
        }

        public boolean actualValidate(@QueryParameter String username, @QueryParameter String apiKey) {
            SauceREST rest = new JenkinsSauceREST(username, apiKey);
            return !"".equals(rest.getUser());
        }
    }

    public final static DomainRequirement DOMAIN_REQUIREMENT = new HostnamePortRequirement("saucelabs.com", 80);

    public static String migrateToCredentials(String username, String accessKey, String migratedFrom) throws InterruptedException, IOException {
        final List<SauceCredentials> credentialsForDomain = SauceCredentials.all((Item) null);
        final StandardUsernameCredentials existingCredentials = CredentialsMatchers.firstOrNull(
            credentialsForDomain,
            CredentialsMatchers.withUsername(username)
        );

        final String credentialId;
        if (existingCredentials == null) {
            String createdCredentialId = UUID.randomUUID().toString();

            final StandardUsernameCredentials credentialsToCreate;
            if (!Strings.isNullOrEmpty(accessKey)) {
                credentialsToCreate = new SauceCredentials(
                    CredentialsScope.GLOBAL,
                    createdCredentialId,
                    username,
                    accessKey,
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
        String key = username + ":" + getPassword().getPlainText();
        return SecurityUtils.hmacEncode("HmacMD5", jobId, key);
    }
}
