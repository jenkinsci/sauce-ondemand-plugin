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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ross Rowe
 */
public class SauceParameterDefinition extends ParameterDefinition {

    private static final Logger logger = Logger.getLogger(SauceParameterDefinition.class.getName());


    @DataBoundConstructor
    public SauceParameterDefinition() {
        super("Sauce Labs Browser", "Sauce Labs Browser description");

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
            return BrowserFactory.getInstance().getWebDriverBrowsers();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Error parsing JSON response", e);
        }
        return Collections.emptyList();
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "Sauce";
        }

        public List<Browser> getWebDriverBrowsers() {
            try {
                return BrowserFactory.getInstance().getWebDriverBrowsers();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.log(Level.SEVERE, "Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }

    }
}
