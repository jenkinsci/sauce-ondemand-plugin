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
import org.apache.commons.codec.binary.Hex;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    /**
     * Could we match session IDs to test names ?
     */
    private final boolean matchingJobNames;

    public SauceOnDemandReport(CaseResult parent, List<String> ids, boolean matchingJobNames) {
        this.parent = parent;
        this.ids = ids;
        this.matchingJobNames = matchingJobNames;
    }

    public AbstractBuild<?,?> getBuild() {
        return parent.getOwner();
    }

    public boolean isMatchingJobNames() {
        return matchingJobNames;
    }

    public List<String> getIDs() {
        return Collections.unmodifiableList(ids);
    }

    public String getId() {
        return ids.get(0);
    }

    public String getAuth() throws IOException {
        return getById(getId()).getAuth();
    }

    public String getIconFileName() {
        return matchingJobNames ? null : "/plugin/sauce-ondemand/images/24x24/video.gif";
    }

    public String getDisplayName() {
        return "Sauce OnDemand report";
    }

    public String getUrlName() {
        return "sauce-ondemand-report";
    }

    public ById getById(String id) {
        return new ById(id);
    }

    public class ById {
        public final String id;

        public ById(String id) {
            this.id = id;
        }

        public String getAuth() throws IOException {
            try {
                DateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd-HH");
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                String dateText = dateFormatGmt.format(new Date());
                
                String key = PluginImpl.get().getUsername() + ":" + PluginImpl.get().getApiKey() + ":" + dateText;
                String msg = id;

                byte[] keyBytes = key.getBytes();
                SecretKeySpec sks = new SecretKeySpec(keyBytes, "HmacMD5");
                Mac mac = Mac.getInstance("HmacMD5");
                mac.init(sks);
                byte[] hmacBytes = mac.doFinal(msg.getBytes());
                byte[] hexBytes = new Hex().encode(hmacBytes);
                String hexStr = new String(hexBytes, "ISO-8859-1");
                return hexStr;
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (InvalidKeyException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (UnsupportedEncodingException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            }

        }

    }
}
