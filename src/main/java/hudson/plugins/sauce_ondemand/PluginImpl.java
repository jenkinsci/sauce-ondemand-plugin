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
import com.saucelabs.saucerest.DataCenter;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.*;
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.StaplerRequest;

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
    static transient final BrowserFactory BROWSER_FACTORY = BrowserFactory.getInstance(new JenkinsSauceREST(null, null, DataCenter.US_WEST));

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

    /**
     * Data center endpoint for Sauce OnDemand.
     */
    @Deprecated
    private transient String restEndpoint;

    private boolean reuseSauceAuth;

    private String sauceConnectDirectory;

    private String sauceConnectOptions;

    private String sauceConnectMaxRetries;

    private String sauceConnectRetryWaitTime;

    @Deprecated
    private transient boolean disableStatusColumn;

    private String environmentVariablePrefix;

    private transient Boolean sendUsageData;

    private boolean disableUsageStats;

    private String credentialId;

    protected Object readResolve() {
        if (sendUsageData != null) {
           disableUsageStats = false;
        }
        return this;
    }

    @Override
    public void start() throws Exception {
        // Register the small (16x16) icons...
        IconSet.icons.addIcon(new Icon(
                "icon-sauce-ondemand-credential icon-sm",
                "sauce-ondemand/images/16x16/sauce-logo-sm.png",
                Icon.ICON_SMALL_STYLE, IconType.PLUGIN)
        );

        // Register the medium (24x24) icons...
        IconSet.icons.addIcon(new Icon(
                "icon-sauce-ondemand-credential icon-md",
                "sauce-ondemand/images/24x24/sauce-logo-md.png",
                Icon.ICON_MEDIUM_STYLE, IconType.PLUGIN)
        );

        // Register the large (32x32) icons...
        IconSet.icons.addIcon(new Icon(
                "icon-sauce-ondemand-credential icon-lg",
                "sauce-ondemand/images/32x32/sauce-logo-lg.png",
                Icon.ICON_LARGE_STYLE, IconType.PLUGIN)
        );

        // Register the x-large (48x48) icons...
        IconSet.icons.addIcon(new Icon(
                "icon-sauce-ondemand-credential icon-xlg",
                "sauce-ondemand/images/48x48/sauce-logo-xlg.png",
                Icon.ICON_XLARGE_STYLE, IconType.PLUGIN)
        );
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
        setDisableUsageStats(formData.getBoolean("disableUsageStats"));
        sauceConnectMaxRetries = formData.getString("sauceConnectMaxRetries");
        sauceConnectRetryWaitTime = formData.getString("sauceConnectRetryWaitTime");
        save();
    }

    public DescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        if (j == null) { return null; }
        return (DescriptorImpl) j.getDescriptorOrDie(PluginImpl.class);
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

    public String getSauceConnectMaxRetries() {
        return sauceConnectMaxRetries;
    }

    public void setSauceConnectMaxRetries(String sauceConnectMaxRetries) {
        this.sauceConnectMaxRetries = sauceConnectMaxRetries;
    }

    public String getSauceConnectRetryWaitTime() {
        return sauceConnectRetryWaitTime;
    }

    public void setSauceConnectRetryWaitTime(String sauceConnectRetryWaitTime) {
        this.sauceConnectRetryWaitTime = sauceConnectRetryWaitTime;
    }

    @Deprecated
    public String getUsername() {
        return username;
    }

    @Deprecated
    public Secret getApiKey() {
        return apiKey;
    }

    @Deprecated
    public String getRestEndpoint() {
        return restEndpoint;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PluginImpl> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand";
        }

        /**
         * @param context    Ancestor/what project
         * @return the list of supported credentials
         */
        public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            return new StandardUsernameListBoxModel()
                .withAll(SauceCredentials.all(context));
        }
    }

    public String getSauceConnectOptions() {
        return sauceConnectOptions;
    }

    public void setSauceConnectOptions(String sauceConnectOptions) {
        this.sauceConnectOptions = sauceConnectOptions;
    }

    public void setDisableUsageStats(boolean disableUsageStats) {
        this.disableUsageStats = disableUsageStats;
    }

    public boolean isDisableUsageStats() {
        return disableUsageStats;
    }
}
