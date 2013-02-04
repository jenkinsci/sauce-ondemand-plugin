package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Project;
import hudson.tasks.BuildWrapper;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;

/**
 * @author Ross Rowe
 */
public class SauceBadgeColumn extends ListViewColumn {

    @DataBoundConstructor
    public SauceBadgeColumn() {
        super();
    }

    public boolean isColumnDisabled() {
        return PluginImpl.get().isDisableStatusColumn();
    }

    public String getSauceUser(@SuppressWarnings("rawtypes") Job job) {

        if (job instanceof Project) {
            Map<Descriptor<BuildWrapper>,BuildWrapper> buildWrappers = ((Project) job).getBuildWrappers();
            for (BuildWrapper buildWrapper : buildWrappers.values()) {
                if (buildWrapper instanceof SauceOnDemandBuildWrapper) {
                    SauceOnDemandBuildWrapper sauceWrapper = (SauceOnDemandBuildWrapper) buildWrapper;
                    return sauceWrapper.getUserName();
                }
            }
        }

        return null;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        @Override
        public boolean shownByDefault() {
            return !PluginImpl.get().isDisableStatusColumn();
        }

        @Override
        public String getDisplayName() {
            return "Sauce Build Status";
        }
    }
}
