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

import hudson.model.*;

/**
 * Test for {@link SauceOnDemandBuildWrapper}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SauceOnDemandBuildWrapperTest extends BaseTezt {

    /**
     * Configuration roundtrip testing.
     */
    public void configRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(new Credentials("username", "accessKey"), new SeleniumInformation("selenium", "http://localhost:8080", null, null), "abc", "1", null, true, false);
        p.getBuildWrappersList().add(before);
        configRoundtrip(p);
        SauceOnDemandBuildWrapper after = p.getBuildWrappersList().get(SauceOnDemandBuildWrapper.class);
        assertEquals(after.getCredentials() , before.getCredentials());
       
    }

    /**
     * Simulates the whole thing.
     */
    public void testFullConfig() throws Exception {
        setCredential();

        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(null, new SeleniumInformation("selenium", "http://localhost:8080", null, null), "localhost", "4445", null, true, true);
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p, new SauceBuilder());
    }

    /**
     * Simulates the whole thing.
     */
    public void minimalConfig() throws Exception {
        setCredential();

        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(null, null, null, "0", null, true, false);
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p, new SauceBuilder());
    }

    //ignore for the moment, as the startup of plexus in the unit tests is failing
    public void runFromSlave() throws Exception {
        setCredential();

        Slave s = createSlave();

        FreeStyleProject p = createFreeStyleProject();
        p.setAssignedNode(s);
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(null, new SeleniumInformation("selenium", "http://localhost", null, null), "localhost", "4445", null, true, false);
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p, new SauceBuilder());
    }

}
