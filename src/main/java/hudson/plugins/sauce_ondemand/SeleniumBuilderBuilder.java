package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.SeleniumBuilderManager;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Ross Rowe
 */
public class SeleniumBuilderBuilder extends Builder {

    private String scriptFile;

    @DataBoundConstructor
    public SeleniumBuilderBuilder(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        SeleniumBuilderManager seleniumBuilderManager = new SeleniumBuilderManager();
        seleniumBuilderManager.executeSeleniumBuilder(false, getScriptFile());

        return true;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Invoke Selenium Builder script";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
