/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.sauce_ondemand;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.ci.SauceLibraryManager;
import com.saucelabs.hudson.HudsonSauceLibraryManager;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.*;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the access credentials and common options for the Sauce plugin.
 *
 * @author Kohsuke Kawaguchi
 * @author Ross Rowe
 */
@Extension
public class PluginImpl extends Plugin implements Describable<PluginImpl> {

    private static final Logger logger = Logger.getLogger(PluginImpl.class.getName());
    /**
     * Handles the retrieval of browsers from Sauce Labs.
     */
    static transient final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null));

    private transient SauceLibraryManager libraryManager = new HudsonSauceLibraryManager();
    /**
     * User name to access Sauce OnDemand.
     */
    @Deprecated
    private transient String username;
    /**
     * Password for Sauce OnDemand.
     */
    @Deprecated
    private transient Secret apiKey;

    private boolean reuseSauceAuth;

    private String sauceConnectDirectory;

    private String sauceConnectOptions;

    @Deprecated
    private transient boolean disableStatusColumn;

    private String environmentVariablePrefix;

    private boolean sendUsageData;

    private String credentialId;

    @Override
    public void start() throws Exception {
        // backward compatibility with the legacy class name
        Items.XSTREAM.alias("hudson.plugins.sauce_ondemand.SoDBuildWrapper", SauceOnDemandBuildWrapper.class);
        Items.XSTREAM.alias("hudson.plugins.sauce__ondemand.SoDBuildWrapper", SauceOnDemandBuildWrapper.class);
        // the real name must be registered at the end
        Items.XSTREAM.alias("hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper", SauceOnDemandBuildWrapper.class);

        load();
    }


    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, Descriptor.FormException {
        sauceConnectDirectory = formData.getString("sauceConnectDirectory");
        sauceConnectOptions = formData.getString("sauceConnectOptions");
        environmentVariablePrefix = formData.getString("environmentVariablePrefix");
        setSendUsageData(formData.getBoolean("sendUsageData"));
        save();

    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static PluginImpl get() {
        Jenkins j = Jenkins.getInstance();
        if (j == null) { return null; }
        return j.getPlugin(PluginImpl.class);
    }

    public String getSauceConnectDirectory() {
        return sauceConnectDirectory;
    }

    public void setSauceConnectDirectory(String sauceConnectDirectory) {
        this.sauceConnectDirectory = sauceConnectDirectory;
    }

    public String getEnvironmentVariablePrefix() {
        return environmentVariablePrefix;
    }

    public void setEnvironmentVariablePrefix(String environmentVariablePrefix) {
        this.environmentVariablePrefix = environmentVariablePrefix;
    }

    @Deprecated
    public String getUsername() {
        return username;
    }

    @Deprecated
    public Secret getApiKey() {
        return apiKey;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PluginImpl> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand";
        }

        /**
         * @return the list of supported credentials
         */
        public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            return new StandardUsernameListBoxModel()
                .withAll(SauceCredentials.all(context));
        }
    }

    /**
     * @return String to show to the user on screen
     */
    @JavaScriptMethod
    public String checkForUpdates() {
        try {
            boolean updateAvailable = libraryManager.checkForLaterVersion();
            return updateAvailable ? "<div>Updates to Sauce Connect are available</div>" +
                    "<a href=\"#\" onclick=\"var progress = document.getElementById('progress');" +
                    "progress.style.display = 'block';" +
                    "plugin.applyUpdates(function(t) {" +
                    "document.getElementById('msg').innerHTML = t.responseObject();" +
                    "var progress = document.getElementById('progress');" +
                    "progress.style.display = 'none';" +
                    "})\">Update Sauce Connect<\\a>" :
                    "No update required, Sauce Connect is up to date";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking for later version", e);
        }
        return "Failed to connect to Sauce OnDemand";
    }

    /**
     * @return Results of applying update
     */
    @JavaScriptMethod
    public String applyUpdates() {
        try {
            libraryManager.triggerReload();
            return "Update of the Sauce Connect library was successful";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error Reloading plugin", e);
        }
        return "Failed to apply updates, please see application logs";
    }

    public String getSauceConnectOptions() {
        return sauceConnectOptions;
    }

    public void setSauceConnectOptions(String sauceConnectOptions) {
        this.sauceConnectOptions = sauceConnectOptions;
    }

    public void setSendUsageData(boolean sendUsageData) {
        this.sendUsageData = sendUsageData;
    }

    public boolean isSendUsageData() {
        return sendUsageData;
    }
}
