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

import com.saucelabs.rest.Credential;
import hudson.plugins.sauce_ondemand.PluginImpl.DescriptorImpl;
import hudson.util.FormValidation.Kind;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImplTest extends HudsonTestCase {
    /**
     * Tests the configuration roundtrip of the access credential.
     */
// what appears to be a bug in core breaks this test. commenting out for now.
//    public void testConfigRoundtrip() throws Exception {
//        PluginImpl p = PluginImpl.get();
//        p.setCredential("foo","bar");
//        submit(createWebClient().goTo("configure").getFormByName("config"));
//        assertEquals("foo",p.getUsername());
//        assertEquals("bar", Secret.toString(p.getApiKey()));
//    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
		File sauceSettings = new File(new File(System.getProperty("user.home")),".sauce-ondemand");
		if (!sauceSettings.exists()) {
			String userName = System.getProperty("sauce.user");
			String accessKey = System.getProperty("access.key");
			Credential credential = new Credential(userName, accessKey);
			credential.saveTo(sauceSettings);
		}
	}

    public void testValidation() throws Exception {
        DescriptorImpl d = PluginImpl.get().getDescriptor();

        // this should fail
        assertEquals(Kind.ERROR, d.doValidate("bogus","bogus", false).kind);

        // this should work
        Credential c = new Credential();
        assertEquals(Kind.OK, d.doValidate(c.getUsername(),c.getKey(), false).kind);
    }
}
