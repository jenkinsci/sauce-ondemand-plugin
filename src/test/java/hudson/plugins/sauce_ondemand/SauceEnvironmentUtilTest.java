package hudson.plugins.sauce_ondemand;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class SauceEnvironmentUtilTest {

    @Test
    public void generateTunnelIdentifier_should_use_the_project_name_in_the_tunnel_id(){
        String expectedProjectName = "My_Project_Name";

        assertThat(SauceEnvironmentUtil.generateTunnelIdentifier(expectedProjectName), containsString(expectedProjectName));
    }

    @Test
    public void generateTunnelIdentifier_should_sanitize_the_project_name(){
        String expectedProjectName = "My Project!@#$%&*()Name-012345689";

        assertThat(SauceEnvironmentUtil.generateTunnelIdentifier(expectedProjectName), containsString("My_Project_Name-012345689"));
    }

    @Test
    public void generateTunnelIdentifier_should_include_an_epoch_timestamp(){
        String expectedProjectName = "My Project";

        String tunnelIdentifier = SauceEnvironmentUtil.generateTunnelIdentifier(expectedProjectName);

        String epochTimeMSStr = tunnelIdentifier.split("-")[1];
        long epochTimeMS = Long.parseLong(epochTimeMSStr);
        long now = System.currentTimeMillis();
        int deltaBetweenNowAndTunnelIdentifier = (int) (now - epochTimeMS);

        assertThat(deltaBetweenNowAndTunnelIdentifier, is(lessThan(12)));
    }
}