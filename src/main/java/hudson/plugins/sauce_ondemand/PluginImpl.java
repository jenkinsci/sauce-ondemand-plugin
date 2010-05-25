package hudson.plugins.sauce_ondemand;

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.SauceTunnelFactory;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Persists the access credential to SoD.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PluginImpl extends Plugin implements Describable<PluginImpl> {
    /**
     * User name to access SoD.
     */
    private String username;
    /**
     * Password for SoD.
     */
    private Secret apiKey;

    public String getUsername() {
        return username;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @Override
    public void start() throws Exception {
        load();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {
        username = formData.getString("username");
        apiKey = Secret.fromString(formData.getString("apiKey"));
        save();
    }

    public Descriptor<PluginImpl> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    public static PluginImpl get() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PluginImpl> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand";
        }

        public FormValidation doValidate(@QueryParameter String username, @QueryParameter String password) {
            try {
                new SauceTunnelFactory(new Credential(username,Secret.toString(Secret.fromString(password)))).list();
                return FormValidation.ok("Success");
            } catch (IOException e) {
                return FormValidation.error(e,"Failed to connect to Sauce OnDemand");
            }
        }
    }
}
