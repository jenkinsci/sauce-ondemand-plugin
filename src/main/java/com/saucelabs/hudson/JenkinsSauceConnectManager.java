package com.saucelabs.hudson;

import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import hudson.plugins.sauce_ondemand.PluginImpl;
import org.apache.commons.lang.StringUtils;

/**
 * @author Ross Rowe
 */
public class JenkinsSauceConnectManager extends SauceConnectTwoManager {

    @Override
    public String getSauceConnectWorkingDirectory() {
        String workingDirectory = PluginImpl.get().getSauceConnectDirectory();
        if (StringUtils.isBlank(workingDirectory)) {
            return super.getSauceConnectWorkingDirectory();
        } else {
            return workingDirectory;
        }

    }
}
