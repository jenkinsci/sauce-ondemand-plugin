package com.saucelabs.hudson;

import com.saucelabs.ci.SauceLibraryManager;
import com.saucelabs.sauceconnect.SauceConnect;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Hudson;
import hudson.plugins.sauce_ondemand.PluginImpl;
import org.apache.commons.io.FileUtils;

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
        File runningJarFile = new File
                (SauceConnect.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        FileUtils.copyFileToDirectory(jarFile, runningJarFile.getParentFile());
        //there doesn't appear to be an easy way to get the .hpi file, so we have to convert the backup file name
        Plugin plugin = Hudson.getInstance().getPlugin(PluginImpl.class);
        PluginWrapper pluginWrapper = plugin.getWrapper();
        File backupFile = pluginWrapper.getBackupFile();
        String backupFileName = backupFile.getName();
        File hpiFile = new File(backupFile.getParentFile(), backupFileName.substring(0, backupFileName.lastIndexOf('.')) + ".hpi");
//        if (hpiFile.exists()) {
//           addFileToJar(hpiFile, new TFile(jarFile));
//        }

    }
}
