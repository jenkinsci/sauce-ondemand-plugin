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

import com.saucelabs.ci.SauceLibraryManager;
import com.saucelabs.hudson.HudsonSauceLibraryManager;
import com.saucelabs.rest.Credential;
import com.saucelabs.rest.SauceTunnelFactory;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Persists the access credential to Sauce OnDemand.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PluginImpl extends Plugin implements Describable<PluginImpl> {
    
    private static final Logger logger = Logger.getLogger(PluginImpl.class);

    private SauceLibraryManager libraryManager = new HudsonSauceLibraryManager();
    /**
     * User name to access Sauce OnDemand.
     */
    private String username;
    /**
     * Password for Sauce OnDemand.
     */
    private Secret apiKey;

    public String getUsername() {
        return username;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @Override
    public void start() throws Exception {
        // backward compatibility with the legacy class name
        Items.XSTREAM.alias("hudson.plugins.sauce_ondemand.SoDBuildWrapper", SauceOnDemandBuildWrapper.class);
        Items.XSTREAM.alias("hudson.plugins.sauce__ondemand.SoDBuildWrapper", SauceOnDemandBuildWrapper.class);
        // the real name must be registered at the end
        Items.XSTREAM.alias("hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper", SauceOnDemandBuildWrapper.class);

        load();
    }

    public void setCredential(String username, String apiKey) throws IOException {
        this.username = username;
        this.apiKey = Secret.fromString(apiKey);
        save();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, Descriptor.FormException {
        username = formData.getString("username");
        apiKey = Secret.fromString(formData.getString("apiKey"));
        save();
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    public static PluginImpl get() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PluginImpl> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand";
        }

        public FormValidation doValidate(@QueryParameter String username, @QueryParameter String apiKey) {
            try {
                new SauceTunnelFactory(new Credential(username, Secret.toString(Secret.fromString(apiKey)))).list();
                return FormValidation.ok("Success");
            } catch (IOException e) {
                return FormValidation.error(e, "Failed to connect to Sauce OnDemand");
            }
        }
    }

    /**
     * 
     * @return
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
            logger.error(e);
        }
        return "Failed to connect to Sauce OnDemand";
    }

    /**
     * 
     * @return
     */
    @JavaScriptMethod
    public String applyUpdates() {
        try {
            libraryManager.triggerReload();
            return "Update of the Sauce Connect library was successful";
        } catch (Exception e) {
            logger.error(e);
        } 
        return "Failed to apply updates, please see application logs";
    }
}
