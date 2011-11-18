package com.saucelabs.ci;


/**
 * Represents a Sauce Browser instance.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class Browser implements Comparable<Browser> {

    private final String key;
    private final String os;
    private final String browserName;
    private final String version;
    private final String name;

    public Browser(String key, String os, String browserName, String version, String name) {
        this.key = key;
        this.os = os;
        this.browserName = browserName;
        this.version = version;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getName() {
        return name;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    public String getPlatform() {
        if (os.equalsIgnoreCase("windows 2008")) {
            //use the 'VISTA' Platform
            return "windows vista";
        }
        //otherwise just return the os
        return os;
    }

    public boolean equals(Object object) {
        if (!(object instanceof Browser)) {
            return false;
        }
        Browser browser = (Browser) object;
        return (key == null ? browser.key == null : key.equals(browser.key)) &&
                (browserName == null ? browser.browserName == null : browserName.equals(browser.browserName)) &&
                (name == null ? browser.name == null : name.equals(browser.name)) &&
                (os == null ? browser.os == null : os.equals(browser.os)) &&
                (version == null ? browser.version == null : version.equals(browser.version));
    }

    public int hashCode() {
        int result = 17;
        if (key != null) {
            result = 31 * result + key.hashCode();
        }
        if (browserName != null) {
            result = 31 * result + browserName.hashCode();
        }
        if (name != null) {
            result = 31 * result + name.hashCode();
        }
        if (os != null) {
            result = 31 * result + os.hashCode();
        }
        if (version != null) {
            result = 31 * result + version.hashCode();
        }
        return result;
    }

    public int compareTo(Browser browser) {
        return String.CASE_INSENSITIVE_ORDER.compare(name, browser.name);
    }

    public String toString() {
        if (name == null) {
            return super.toString();
        } else {
            return name;
        }
    }
    
    /**
     * Returns the driver URI string.
     */
    public String getUri() {
        return "sauce-ondemand:?os="+os+"&browser="+browserName+"&browser-version="+version;
    }
}
