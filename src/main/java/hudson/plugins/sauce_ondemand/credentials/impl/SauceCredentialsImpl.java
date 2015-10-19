package hudson.plugins.sauce_ondemand.credentials.impl;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
                            @NonNull String username, @NonNull Secret apiKey, @CheckForNull String description) {
        super(scope, id, description);
        this.apiKey = apiKey;
        this.username = username;
    }

    @NonNull
    public Secret getPassword() {
        return this.apiKey;
    }

    @NonNull
    public String getUsername() {
        return this.getUsername();
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor
    {
        @Override
        public String getDisplayName() {
            return "Sauce Labs";
        }
    }
}
