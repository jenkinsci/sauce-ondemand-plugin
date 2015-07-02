package hudson.plugins.sauce_ondemand;

import hudson.model.ParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Ross Rowe
 */
public class SauceBrowserResolutionParameterValue extends ParameterValue {

    private final String resolution;

    @DataBoundConstructor
    public SauceBrowserResolutionParameterValue(String name, String resolution) {
        super(name);
        this.resolution = resolution;
    }
}
