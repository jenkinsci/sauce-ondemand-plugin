package hudson.plugins.sauce_ondemand;

import hudson.model.AbstractBuild;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Ross Rowe
 */
public class SauceBrowserResolutionParameterDefinition extends ParameterDefinition {

    @DataBoundConstructor
    public SauceBrowserResolutionParameterDefinition() {
        super("Sauce Labs Browser Resolution", "Select the resolution that should be used with the selected Sauce Labs browser(s)");

    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        throw new RuntimeException("Not supported");
    }

    //@Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        /**
         * @return label to be displayed within the list of parameter options
         */
        @Override
        public String getDisplayName() {
            return "Sauce Labs Browser Resolution";
        }

    }

    private SauceOnDemandBuildWrapper getBuildWrapper(AbstractBuild<?, ?> build) {
            return SauceEnvironmentUtil.getBuildWrapper(build.getProject());
        }

    public void getSelectedBrowsers() {

    }
}
