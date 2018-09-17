package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows users to select Sauce browsers as parameters for a Jenkins build.
 *
 * @author Ross Rowe
 */
public class SauceParameterDefinition extends ParameterDefinition {

    /** Logger instance.*/
    private static final Logger logger = Logger.getLogger(SauceParameterDefinition.class.getName());

    /** Handles the retrieval of browsers from Sauce Labs. */
    private static final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

    @DataBoundConstructor
    public SauceParameterDefinition() {
        super("Sauce Labs Browsers", "Select the browser(s) that should be used when tests are run with Sauce Labs");

    }

    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jo) {

        JSONArray selectedBrowsers = jo.getJSONArray("webDriverBrowsers");
        return new SauceParameterValue(getName(), selectedBrowsers);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request) {
        throw new RuntimeException("Not supported");
    }

    public List<Browser> getWebDriverBrowsers() {
        try {
            return BROWSER_FACTORY.getWebDriverBrowsers();
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Error parsing JSON response", e);
        }
        return Collections.emptyList();
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        /**
         *
         * @return label to be displayed within the list of parameter options
         */
        @Override
        public String getDisplayName() {
            return "Sauce Labs Browsers";
        }

    }
}
