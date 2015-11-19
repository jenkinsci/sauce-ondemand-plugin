package hudson.plugins.sauce_ondemand.credentials.impl;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.base.Strings;
import com.saucelabs.saucerest.SauceREST;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.plugins.sauce_ondemand.JenkinsSauceREST;
import hudson.plugins.sauce_ondemand.PluginImpl;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.apache.commons.codec.binary.Hex;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by gavinmogan on 10/17/15.
 */
public class SauceCredentialsImpl extends BaseStandardCredentials implements StandardUsernamePasswordCredentials {
    /**
     * Ensure consistent serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The username.
     */
    protected final String username;

    /**
     * The Password/apikey
     */
    protected final Secret apiKey;

    /**
     *
     * @param scope
     * @param id
     * @param username
     * @param apiKey
     * @param description
     */
    @DataBoundConstructor
    public SauceCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                            @NonNull String username, @NonNull String apiKey, @CheckForNull String description) {
        super(scope, id, description);
        this.apiKey = Secret.fromString(apiKey);
        this.username = username;
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
        return "SauceCredentialsImpl{" +
            "apiKey=" + apiKey +
            ", username='" + username + '\'' +
            '}';
    }

    public static SauceCredentialsImpl getSauceCredentials(AbstractBuild build, SauceOnDemandBuildWrapper wrapper) {
        String credentialId = !Strings.isNullOrEmpty(wrapper.getCredentialId()) ? wrapper.getCredentialId() : PluginImpl.get().getCredentialId();
        return getCredentialsById(build.getProject(), credentialId);
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
            return FormValidation.error("Bad username or api key");
        }

        public boolean actualValidate(@QueryParameter String username, @QueryParameter String apiKey) {
            SauceREST rest = new JenkinsSauceREST(username, apiKey);
            if ("".equals(rest.getUser())) {
                return false;
            }
            return true;
        }
    }

    public final static DomainRequirement DOMAIN_REQUIREMENT = new HostnamePortRequirement("saucelabs.com", 80);

    public static String migrateToCredentials(String username, String accessKey, String migratedFrom) throws InterruptedException, IOException {
        final List<SauceCredentialsImpl> credentialsForDomain = SauceCredentialsImpl.all((Item) null);
        final StandardUsernameCredentials existingCredentials = CredentialsMatchers.firstOrNull(
            credentialsForDomain,
            CredentialsMatchers.withUsername(username)
        );

        final String credentialId;
        if (existingCredentials == null) {
            String createdCredentialId = UUID.randomUUID().toString();

            final StandardUsernameCredentials credentialsToCreate;
            if (!Strings.isNullOrEmpty(accessKey)) {
                credentialsToCreate = new SauceCredentialsImpl(
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

    public static List<SauceCredentialsImpl> all(ItemGroup context) {
        return CredentialsProvider.lookupCredentials(
            SauceCredentialsImpl.class,
            context,
            ACL.SYSTEM,
            SauceCredentialsImpl.DOMAIN_REQUIREMENT
        );
    }

    public static List<SauceCredentialsImpl> all(Item context) {
        return CredentialsProvider.lookupCredentials(
            SauceCredentialsImpl.class,
            context,
            ACL.SYSTEM,
            SauceCredentialsImpl.DOMAIN_REQUIREMENT
        );
    }

    public static SauceCredentialsImpl getCredentialsById(Item context, String id) {
        return CredentialsMatchers.firstOrNull(
            SauceCredentialsImpl.all((Item) context),
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
     * @throws java.security.NoSuchAlgorithmException FIXME
     * @throws java.security.InvalidKeyException FIXME
     * @throws java.io.UnsupportedEncodingException FIXME
     *
     */
    public String getHMAC(String jobId) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String key = username + ":" + this.getApiKey().getPlainText() + ":" + format.format(calendar.getTime());
        byte[] keyBytes = key.getBytes();
        SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
        Mac mac = Mac.getInstance(sks.getAlgorithm());
        mac.init(sks);
        byte[] hmacBytes = mac.doFinal(jobId.getBytes());
        byte[] hexBytes = new Hex().encode(hmacBytes);
        return new String(hexBytes, "ISO-8859-1");
    }
}
