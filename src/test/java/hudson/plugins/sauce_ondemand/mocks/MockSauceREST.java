package hudson.plugins.sauce_ondemand.mocks;

import com.saucelabs.saucerest.SauceREST;
import hudson.plugins.sauce_ondemand.JenkinsSauceREST;

import java.net.URL;

public class MockSauceREST extends JenkinsSauceREST {

    public MockSauceREST() {
        super("fake", "", "US");
    }

    @Override
    public String retrieveResults(URL restEndpoint) {
        throw new RuntimeException("Shouldn't actually hit server when testing");
    }
}
