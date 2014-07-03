package hudson.plugins.sauce_ondemand;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Ross Rowe
 */
public class SauceConnectFourIT {

    /**
     * JUnit rule which instantiates a local Jenkins instance with our plugin installed.
     */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * @throws Exception thrown if an unexpected error occurs
     */
    @Before
    public void setUp() throws Exception {

    }


    @Test
    @Ignore
    public void noOptions() {

    }

    @Test
    @Ignore
    public void tunnelIdentifier() {

    }

}
