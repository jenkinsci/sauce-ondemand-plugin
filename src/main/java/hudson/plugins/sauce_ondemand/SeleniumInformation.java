package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Ross Rowe
 */
public class SeleniumInformation {

    @DataBoundConstructor
    public SeleniumInformation(String startingURL) {
        this.startingURL = startingURL;
    }
    
    public String getStartingURL() {
        return startingURL;
    }

    public void setStartingURL(String startingURL) {
        this.startingURL = startingURL;
    }

    private String startingURL;


}
