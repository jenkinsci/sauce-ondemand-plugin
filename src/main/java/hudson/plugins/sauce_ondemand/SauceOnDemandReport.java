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
import hudson.plugins.sauce_ondemand.credentials.SauceCredentials;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    @Deprecated
    private final String userName = null;
    @Deprecated
    private final String apiKey = null;

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
        return new SauceTestResultsById(getId(), SauceCredentials.getCredentials(getBuild())).getAuth();
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
