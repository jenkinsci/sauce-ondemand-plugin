package hudson.plugins.sauce_ondemand.credentials;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class SauceCredentialsEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        super.buildEnvironmentFor(r, envs, listener);

        Job parent = r.getParent();
        if (parent instanceof AbstractProject) {
            SauceCredentials creds = SauceCredentials.getCredentials((AbstractProject)parent);
            if (creds != null) {
                envs.put(SauceOnDemandBuildWrapper.SAUCE_USERNAME, creds.getUsername());
                envs.put(SauceOnDemandBuildWrapper.SAUCE_ACCESS_KEY, creds.getPassword().getPlainText());
            }
        }
    }
}
