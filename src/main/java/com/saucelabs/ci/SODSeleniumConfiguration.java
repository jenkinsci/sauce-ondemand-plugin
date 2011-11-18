package com.saucelabs.ci;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class SODSeleniumConfiguration
{
    private static final Logger logger = Logger.getLogger(SODSeleniumConfiguration.class);

    private String username;
    private String accessKey;
    private Browser browser;
    private String jobName;
    private boolean recordVideo;
    private List<String> userExtensions;
    private String firefoxProfileUrl;
    private int maxDuration;
    private int idleTimeout;

    public SODSeleniumConfiguration(String username, String accessKey, Browser browser)
    {
        this.username = username;
        this.accessKey = accessKey;
        this.browser = browser;
        this.userExtensions = new ArrayList<String>();
    }

    public String toJson() throws JSONException
    {
        //this is just a simple encoder.
        //no need for complex json utils at the moment
        JSONObject config = new JSONObject();
        config.put("username",username);
        config.put("access-key",accessKey);
        if (browser != null) {
            config.put("os",browser.getOs());
            config.put("browser",browser.getBrowserName());
            config.put("browser-version",browser.getVersion());
        }
        config.put("record-video",recordVideo);

        if(StringUtils.isNotBlank(jobName)) {
            config.put("job-name",jobName);
        }

        if(StringUtils.isNotBlank(firefoxProfileUrl)) {
            config.put("firefox-profile-url",firefoxProfileUrl);
        }

        if(userExtensions.size() > 0) {
            JSONArray extArray = new JSONArray(userExtensions);
            config.put("user-extensions-url",extArray);
        }

        if(maxDuration > 0) {
            config.put("max-duration",maxDuration);
        }

        if(idleTimeout > 0) {
            config.put("idle-timeout",idleTimeout);
        }

        return StringEscapeUtils.escapeJava(config.toString());

    }

    public void addUserExtension(String ext) {
        userExtensions.add(ext);
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    public Browser getBrowser()
    {
        return browser;
    }

    public void setBrowser(Browser browser)
    {
        this.browser = browser;
    }

    public String getFirefoxProfileUrl()
    {
        return firefoxProfileUrl;
    }

    public void setFirefoxProfileUrl(String firefoxProfileUrl)
    {
        this.firefoxProfileUrl = firefoxProfileUrl;
    }

    public int getIdleTimeout()
    {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }

    public String getJobName()
    {
        return jobName;
    }

    public void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public int getMaxDuration()
    {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration)
    {
        this.maxDuration = maxDuration;
    }

    public boolean isRecordVideo()
    {
        return recordVideo;
    }

    public void setRecordVideo(boolean recordVideo)
    {
        this.recordVideo = recordVideo;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public List<String> getUserExtensions()
    {
        return userExtensions;
    }

    public void setUserExtensions(List<String> userExtensions)
    {
        this.userExtensions = userExtensions;
    }

    public void setUserExtensions(JSONArray userExtensionsJson)
    {

        for(int i=0;i<userExtensionsJson.length();i++) {
            try
            {
                String ext = userExtensionsJson.getString(i);
                this.userExtensions.add(ext);
            } catch (JSONException e)
            {
                //just print and ignore
                logger.error("Error parsing JSON string", e);
            }
        }
    }

    public void setUserExtensions(String jsonString)
    {
        if(StringUtils.isNotBlank(jsonString)) {
            try
            {
                setUserExtensions(new JSONArray(jsonString));
            } catch (JSONException e)
            {
                //just print and ignore
                logger.error("Error parsing JSON string", e);
            }
        }

    }


}
