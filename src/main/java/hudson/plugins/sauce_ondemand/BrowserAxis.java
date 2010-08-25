package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class BrowserAxis extends Axis {
    @DataBoundConstructor
    public BrowserAxis(List<String> values) {
        super("SELENIUM_DRIVER", values);
    }

    // TODO: more hooks to inject variables and values
    // TODO: matrix or a list as the UI?

//    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Cross-browser tests";
        }
    }
}
