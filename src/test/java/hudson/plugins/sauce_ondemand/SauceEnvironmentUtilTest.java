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

        AbstractProject project = makeMockProject(expectedProjectName);
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getProject()).thenReturn(project);

        String tunnelIdentifier = SauceEnvironmentUtil.generateTunnelIdentifier(build);

        assertThat(tunnelIdentifier, containsString(expectedProjectName));
        verify(build).getProject();
        verify(project).getName();
    }

    @Test
    public void generateTunnelIdentifier_should_sanitize_the_project_name(){
        String expectedProjectName = "My Project!@#$%&*()Name-012345689";

        AbstractProject project = makeMockProject(expectedProjectName);
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getProject()).thenReturn(project);

        String tunnelIdentifier = SauceEnvironmentUtil.generateTunnelIdentifier(build);

        assertThat(tunnelIdentifier, containsString("My_Project_Name-012345689"));
        verify(build).getProject();
        verify(project).getName();
    }

    @Test
    public void generateTunnelIdentifier_should_include_an_epoch_timestamp(){
        String expectedProjectName = "My Project";

        AbstractProject project = makeMockProject(expectedProjectName);
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getProject()).thenReturn(project);

        String tunnelIdentifier = SauceEnvironmentUtil.generateTunnelIdentifier(build);

        String epochTimeMSStr = tunnelIdentifier.split("-")[1];
        long epochTimeMS = Long.parseLong(epochTimeMSStr);
        long now = System.currentTimeMillis();
        int deltaBetweenNowAndTunnelIdentifier = (int) (now - epochTimeMS);

        assertThat(deltaBetweenNowAndTunnelIdentifier, is(lessThan(10)));
    }

    private AbstractProject makeMockProject(String projectName) {
        AbstractProject project = mock(AbstractProject.class);
        when(project.getName()).thenReturn(projectName);
        return project;
    }
}