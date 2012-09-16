package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.reporters.Messages;
import hudson.maven.reporters.SurefireArchiver;
import hudson.model.Descriptor;
import hudson.tasks.junit.TestDataPublisher;

/**
 * @author Ross Rowe
 */
public class SauceOnDemandSurefireArchiver extends SurefireArchiver {

    @Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public String getDisplayName() {
            return "Embed Sauce OnDemand reports";
        }

        public SurefireArchiver newAutoInstance(MavenModule module) {
            return new SauceOnDemandSurefireArchiver();
        }
    }
}
