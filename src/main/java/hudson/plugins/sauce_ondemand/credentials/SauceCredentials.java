package hudson.plugins.sauce_ondemand.credentials;

import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

import javax.annotation.Nonnull;

/**
 * Created by gavinmogan on 10/17/15.
 */
@NameWith(SauceCredentials.NameProvider.class)
public interface SauceCredentials extends StandardCredentials {
    @Nonnull
    Secret getSecret();

    class NameProvider extends CredentialsNameProvider<SauceCredentials>
    {
        @Override
        public String getName(SauceCredentials c)
        {
            String description = StringUtils.stripToEmpty(c.getDescription());
            return "SauceLabs " + (description != null ? " ( " + description + ")" : "");
        }
    }
}
