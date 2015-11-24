package hudson.plugins.sauce_ondemand.mocks;

import com.saucelabs.saucerest.SauceREST;
import hudson.plugins.sauce_ondemand.JenkinsSauceREST;

import java.net.URL;

/**
 * Created by gavinmogan on 2015-11-23.
 */
public class MockSauceREST extends JenkinsSauceREST {

    public MockSauceREST() {
        super("fake", "");
    }

    @Override
    public String retrieveResults(URL restEndpoint) {
        return super.retrieveResults(restEndpoint);
    }
}
