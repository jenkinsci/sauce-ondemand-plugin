package com.saucelabs.hudson;

import com.saucelabs.ci.SauceLibraryManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Ross Rowe
 */
public class HudsonSauceLibraryManager extends SauceLibraryManager {
    /**
     * 
     * @param jarFile
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public void updatePluginJar(File jarFile) throws IOException, URISyntaxException {


    }
}
