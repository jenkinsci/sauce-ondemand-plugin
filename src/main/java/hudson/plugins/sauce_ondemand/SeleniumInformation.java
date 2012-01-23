package hudson.plugins.sauce_ondemand;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Ross Rowe
 */
public class SeleniumInformation implements Serializable {

    private String startingURL;

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


    @Override
    public int hashCode() {
        int result = 17;
        if (startingURL != null) {
            result = 31 * result + startingURL.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SeleniumInformation)) {
            return false;
        }
        SeleniumInformation seleniumInformation = (SeleniumInformation) object;
        return (startingURL == null ? seleniumInformation.startingURL == null : startingURL.equals(seleniumInformation.startingURL));
    }

    @Override
    public String toString() {
        return startingURL == null ? super.toString() : startingURL;
    }
}
