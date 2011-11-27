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
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(new Tunnel(1, "abc"), new Tunnel(2,"def"));
        p.getBuildWrappersList().add(before);
        configRoundtrip(p);
        SauceOnDemandBuildWrapper after = p.getBuildWrappersList().get(SauceOnDemandBuildWrapper.class);

        assertEquals(before.getTunnels().size(), after.getTunnels().size());
        for (int i = 0; i < before.getTunnels().size(); i++) {
            Tunnel b = before.getTunnels().get(i);
            Tunnel a = after.getTunnels().get(i);
            assertEqualBeans(a,b,"localPort,localHost");
        }
    }

    /**
     * Simulates the whole thing.
     */
    public void testAutoHostName() throws Exception {
        setCredential();

        FreeStyleProject p = createFreeStyleProject();
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(new Tunnel(8080, "localhost"));
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p, new SauceBuilder());
    }

    public void testRunFromSlave() throws Exception {
        setCredential();

        Slave s = createSlave();

        FreeStyleProject p = createFreeStyleProject();
        p.setAssignedNode(s);
        SauceOnDemandBuildWrapper before = new SauceOnDemandBuildWrapper(new Tunnel(8080, "localhost"));
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p, new SauceBuilder());
    }

}
