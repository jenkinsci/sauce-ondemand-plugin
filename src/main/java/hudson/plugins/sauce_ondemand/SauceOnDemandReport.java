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

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.util.TimeUnit2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Show videos for the tests.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReport extends TestAction {
    public final CaseResult parent;
    /**
     * Session IDs.
     */
    private final List<String> ids;

    public SauceOnDemandReport(CaseResult parent, List<String> ids) {
        this.parent = parent;
        this.ids = ids;
    }

    public AbstractBuild<?,?> getBuild() {
        return parent.getOwner();
    }

    public List<String> getIDs() {
        return Collections.unmodifiableList(ids);
    }

    public String getIconFileName() {
        return "/plugin/sauce-ondemand/images/24x24/video.gif";
    }

    public String getDisplayName() {
        return "Video & Server Log";
    }

    public String getUrlName() {
        return "sauce-ondemand-report";
    }

    public ById getById(String id) {
        return new ById(id);
    }

    public class ById {
        public final String id;
        private final DownloadQueue d = PluginImpl.get().download;

        public ById(String id) {
            this.id = id;
        }

        @WebMethod(name="video.flv")
        public void doVideo(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            serve(req,rsp,"video.flv");
        }

        @WebMethod(name="selenium-server.log")
        public void doServerLog(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            serve(req,rsp,"selenium-server.log");
        }

        public boolean hasVideo() {
            return d.toLocalFile(getBuild(), id, "video.flv").exists();
        }

        private void serve(StaplerRequest req, StaplerResponse rsp, String fileName) throws IOException, ServletException {
            File f = d.toLocalFile(getBuild(), id, fileName);
            if (f.exists()) {// already downloaded. serve it
                rsp.serveFile(req,f.toURI().toURL(), TimeUnit2.DAYS.toMillis(365));
            } else {
                // try to fetch right away but for the time being, fail
                withRequest();
                rsp.sendError(StaplerResponse.SC_SERVICE_UNAVAILABLE,"Not downloaded yet. Please check back");
            }
        }

        /**
         * Requests a download now.
         */
        public ById withRequest() {
            d.requestHighPriority(id,getBuild());
            return this;
        }
    }
}
