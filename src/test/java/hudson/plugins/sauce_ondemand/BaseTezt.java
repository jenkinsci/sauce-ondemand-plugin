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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Test for {@link hudson.plugins.sauce_ondemand.SauceOnDemandBuildWrapper}.
 *
 * @author Kohsuke Kawaguchi
 */
public class BaseTezt extends HudsonTestCase {
    Server server;
    int jettyLocalPort;
    final int secret = new Random().nextInt();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new Server(8080);
		File sauceSettings = new File(new File(System.getProperty("user.home")),".sauce-ondemand");
		if (!sauceSettings.exists()) {
			String userName = System.getProperty("sauce.user");
			String accessKey = System.getProperty("access.key");
			Credential credential = new Credential(userName, accessKey);
			credential.saveTo(sauceSettings);
		}
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/html");
                resp.getWriter().println("<html><head><title>test" + secret + "</title></head><body>it works</body></html>");
            }
        }), "/");
        server.setHandler(handler);

        SocketConnector connector = new SocketConnector();
        server.addConnector(connector);
        server.start();
        jettyLocalPort = connector.getLocalPort();
        System.out.println("Started Jetty at "+ jettyLocalPort);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } catch(IncompatibleClassChangeError e) {
            //do nothing, attempt to continue
        } 
        server.stop();
    }

    /**
     * Sets the Sauce OnDemand credential from ~/.sauce-ondemand.
     * For the tests to run, this file needs to be present.
     */
    void setCredential() throws IOException {
        Credential c = new Credential();
        PluginImpl.get().setCredential(c.getUsername(),c.getKey());
    }

    /**
     * Executes a Selenium test as if it were run from the build.
     */
    void invokeSeleniumFromBuild(FreeStyleProject p, SauceBuilder sauceBuilder) throws Exception {
        p.getBuildersList().add(sauceBuilder);
        buildAndAssertSuccess(p);
    }

    class SauceBuilder extends TestBuilder {

        String sessionId;
        private String testReport;

        public SauceBuilder() {}

        public SauceBuilder(String testReport) {
            this.testReport = testReport;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            sessionId = new TeztSimulation(build, listener, secret, testReport).doTest();
            return true;
        }
    }
}
