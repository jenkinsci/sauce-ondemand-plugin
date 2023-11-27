package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.Browser;
import hudson.Extension;
import hudson.matrix.AxisDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Multi-configuration project axis for Appium browsers.
 *
 * @author Ross Rowe
 */
public class AppiumAxis extends BrowserAxis {

    /** Logger instance. */
    private static final Logger logger = Logger.getLogger(WebDriverAxis.class.getName());

    /**
     * Constructs a new instance.
     * @param values list of Appium browsers
     */
    @DataBoundConstructor
    public AppiumAxis(List<String> values) {
        super(values);
    }

    /**
     *
     * @param value browser key
     * @return Appium browser which corresponds to key
     */
    @Override
    protected Browser getBrowserForKey(String value) {
        return BROWSER_FACTORY.appiumBrowserForKey(value);
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        /**
         *
         * @return label to be displayed in supported multi-configuration axis list
         */
        @Override
        public String getDisplayName() {
            return "Sauce Labs Appium tests";
        }

        /**
         *
         * @return list of Appium browsers
         */
        public List<Browser> getBrowsers() {
            try {
                return BROWSER_FACTORY.getAppiumBrowsers();
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Error parsing JSON response", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error retrieving response", e);
            }
            return Collections.emptyList();
        }
    }
}
