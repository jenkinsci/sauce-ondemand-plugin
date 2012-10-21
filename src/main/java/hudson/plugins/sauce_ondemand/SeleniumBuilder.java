package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.maven.project.MavenProject;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Ross Rowe
 */
public class SeleniumBuilder extends Builder {

    @DataBoundConstructor
    public SeleniumBuilder() {

    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType) ||
                    MavenProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Run Selenium Builder";
        }
    }
}
