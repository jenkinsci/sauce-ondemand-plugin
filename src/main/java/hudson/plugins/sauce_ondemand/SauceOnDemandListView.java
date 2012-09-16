package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.views.ListViewColumn;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Ross Rowe
 */
public class SauceOnDemandListView extends ListViewColumn {

    @DataBoundConstructor
    public SauceOnDemandListView() {
    }

    @Override
    public String getColumnCaption() {
        return getDescriptor().getDisplayName();
    }

    //@Extension
    public static final class DescriptorImpl extends Descriptor<ListViewColumn> {

        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Results";
        }
    }
}
