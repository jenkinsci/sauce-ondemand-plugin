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
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Show videos for the tests.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandReport extends TestAction {

    private static final Logger logger = Logger.getLogger(SauceOnDemandReport.class.getName());

    public final CaseResult parent;
    /**
     * Session IDs.
     */
    private final List<String[]> sessionIds;

    private static final String HMAC_KEY = "HMACMD5";

    public SauceOnDemandReport(CaseResult parent, List<String[]> ids) {
        this.parent = parent;
        this.sessionIds = ids;
    }

    public AbstractBuild<?, ?> getBuild() {
        return parent.getOwner();
    }


    public List<String> getIDs() {
        logger.fine("Retrieving Sauce job ids");
        List<String> ids = new ArrayList<String>();
        for (String[] sessionId : sessionIds) {
            ids.add(sessionId[0]);
        }
        return Collections.unmodifiableList(ids);
    }

    public String getId() {
        return getIDs().get(0);
    }

    public String getAuth() throws IOException {
        return getById(getId()).getAuth();
    }

    public String getIconFileName() {
        return "/plugin/sauce-ondemand/images/24x24/video.gif";
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
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String key = PluginImpl.get().getUsername() + ":" + PluginImpl.get().getApiKey() + ":" + format.format(calendar.getTime());
                byte[] keyBytes = key.getBytes();
                SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
                Mac mac = Mac.getInstance(sks.getAlgorithm());
                mac.init(sks);
                byte[] hmacBytes = mac.doFinal(id.getBytes());
                byte[] hexBytes = new Hex().encode(hmacBytes);
                return new String(hexBytes, "ISO-8859-1");


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
