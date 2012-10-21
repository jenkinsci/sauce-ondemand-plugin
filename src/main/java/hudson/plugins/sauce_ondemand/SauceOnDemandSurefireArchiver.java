package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.maven.*;
import hudson.maven.reporters.Messages;
import hudson.maven.reporters.SurefireArchiver;
import hudson.maven.reporters.SurefireReport;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.junit.TestDataPublisher;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

/**
 * @author Ross Rowe
 */
public class SauceOnDemandSurefireArchiver extends SurefireArchiver {

    @Override
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        System.out.println("Inside Sauce postExecute");
        boolean result = super.postExecute(build, pom, mojo, listener, error);
        if (build instanceof MavenBuild.ProxyImpl2) {
            MavenBuild.ProxyImpl2 proxy = (MavenBuild.ProxyImpl2) build;
            MavenBuild mavenBuild = proxy.owner();
            SurefireReport sr = mavenBuild.getAction(SurefireReport.class);
            mavenBuild.getParentBuild().addAction(sr);
        } else if (build instanceof MavenBuildProxy2.Filter) {
            MavenBuildProxy2.Filter filter = (MavenBuildProxy2.Filter) build;

        }
        return result;
    }

    //@Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public String getDisplayName() {
            return "Embed Sauce OnDemand reports";
        }

        public SurefireArchiver newAutoInstance(MavenModule module) {
            return new SauceOnDemandSurefireArchiver();
        }
    }
}
