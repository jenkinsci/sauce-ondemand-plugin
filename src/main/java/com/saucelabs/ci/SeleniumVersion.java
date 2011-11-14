package com.saucelabs.ci;

/**
 * Used to represent different versions of Selenium.
 * @author Ross Rowe
 */
public enum SeleniumVersion {
    ONE("1.x"), TWO("2.x");
    private String versionNumber;

    SeleniumVersion(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVersionNumber() {
        return versionNumber;
    }
}