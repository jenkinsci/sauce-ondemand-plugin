package com.saucelabs.jenkins;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import org.apache.commons.lang.StringUtils;

/**
 * @author Ross Rowe
 */
public class HudsonSauceConnectFourManager extends SauceConnectFourManager {

    private String workingDirectory;

    @Override
    public String getSauceConnectWorkingDirectory() {
        if (StringUtils.isBlank(workingDirectory)) {
            return super.getSauceConnectWorkingDirectory();
        } else {
            return workingDirectory;
        }

    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
