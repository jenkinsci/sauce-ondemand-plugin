/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.saucelabs.ci;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles invoking the Sauce REST API to retrieve the list of valid Browsers.  The list of browser is cached for
 * an hour.
 *
 * @author Ross Rowe
 */
public class BrowserFactory {

    private static final Logger logger = Logger.getLogger(BrowserFactory.class);

    public static final String BROWSER_URL = "http://saucelabs.com/rest/v1/info/browsers";

    private Map<String, Browser> lookup = new HashMap<String, Browser>();
    protected Timestamp lastLookup = null;
    private static final String IEHTA = "iehta";
    private static final String CHROME = "chrome";
	private static BrowserFactory instance;

    public BrowserFactory() {
        try {
            initializeBrowsers();
        } catch (IOException e) {
            //TODO exception could mean we're behind firewall
            logger.error("Error retrieving browsers, attempting to continue", e);
        } catch (JSONException e) {
            logger.error("Error retrieving browsers, attempting to continue", e);
        }
    }

    public  List<Browser> values() throws IOException, JSONException {
        List<Browser> browsers;
        if (shouldRetrieveBrowsers()) {
            browsers = initializeBrowsers();
        } else {
            browsers = new ArrayList<Browser>(lookup.values());
        }
        Collections.sort(browsers);

        return browsers;
    }

    public boolean shouldRetrieveBrowsers() {
        return lastLookup == null;
    }

    private List<Browser> initializeBrowsers() throws IOException, JSONException {
        List<Browser> browsers = getBrowsersFromSauceLabs();
        lookup = new HashMap<String,Browser>();
        for (Browser browser : browsers) {
            lookup.put(browser.getKey(), browser);
        }
        lastLookup = new Timestamp(new Date().getTime());
        return browsers;
    }

    private List<Browser> getBrowsersFromSauceLabs() throws IOException, JSONException {
         String response = getSauceAPIFactory().doREST(BROWSER_URL);
         return getBrowserListFromJson(response);
    }
    
    public SauceFactory getSauceAPIFactory() {
        return new SauceFactory();
    }

    /**
     * Parses the JSON response and constructs a List of {@link Browser} instances.
     *
     * @param browserListJson
     * @return
     * @throws JSONException
     */
    public List<Browser> getBrowserListFromJson(String browserListJson) throws JSONException {
        List<Browser> browsers = new ArrayList<Browser>();

        JSONArray browserArray = new JSONArray(browserListJson);
        for (int i = 0; i < browserArray.length(); i++) {
            JSONObject browserObject = browserArray.getJSONObject(i);
            String seleniumName = browserObject.getString("selenium_name");
            if (seleniumName.equals(IEHTA) || seleniumName.equals(CHROME)) {
                //exclude these browsers from the list, as they replicate iexplore and firefox
                continue;
            }
            String longName = browserObject.getString("long_name");
            String longVersion = browserObject.getString("long_version");
            String osName = browserObject.getString("os");
            String shortVersion = browserObject.getString("short_version");
            String browserKey = osName + seleniumName + shortVersion;
            String label = osName + " " + longName + " " + longVersion;
            browsers.add(new Browser(browserKey, osName, seleniumName, shortVersion, label));
        }
        return browsers;
    }

    public Browser forKey(String key) {
        return lookup.get(key);
    }

	/**
     * Returns a singleton instance of SauceFactory.  This is required because
     * remote agents don't have the Bamboo component plugin available, so the Spring
     * auto-wiring doesn't work. 
     * @return
     */
    public static BrowserFactory getInstance() {
        if (instance == null) {
            instance = new BrowserFactory();
        }
        return instance;
    }

}
