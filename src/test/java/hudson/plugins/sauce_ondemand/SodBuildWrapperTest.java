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
import com.thoughtworks.selenium.DefaultSelenium;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
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
import java.io.IOException;
import java.util.Random;

/**
 * Test for {@link SoDBuildWrapper}.
 *
 * @author Kohsuke Kawaguchi
 */
public class SodBuildWrapperTest extends HudsonTestCase {
    private Server server;
    private int jettyLocalPort;
    private final int secret = new Random().nextInt();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new Server();
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/html");
                resp.getWriter().println("<html><head><title>test"+secret+"</title></head><body>it works</body></html>");
            }
        }),"/");
        server.setHandler(handler);

        SocketConnector connector = new SocketConnector();
        server.addConnector(connector);
        server.start();
        jettyLocalPort = connector.getLocalPort();
        System.out.println("Started Jetty at "+ jettyLocalPort);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    /**
     * Configuration roundtrip testing.
     */
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        SoDBuildWrapper before = new SoDBuildWrapper(new Tunnel(1, 2, "abc", "def"), new Tunnel(3, 4, "ghi", "jkl"));
        p.getBuildWrappersList().add(before);
        configRoundtrip(p);
        SoDBuildWrapper after = p.getBuildWrappersList().get(SoDBuildWrapper.class);

        assertEquals(before.getTunnels().size(), after.getTunnels().size());
        for (int i = 0; i < before.getTunnels().size(); i++) {
            Tunnel b = before.getTunnels().get(i);
            Tunnel a = after.getTunnels().get(i);
            assertEqualBeans(a,b,"remotePort,localPort,localHost,domains");
        }
    }

    /**
     * Simulates the whole thing.
     */
    public void testAutoHostName() throws Exception {
        setCredential();

        FreeStyleProject p = createFreeStyleProject();
        SoDBuildWrapper before = new SoDBuildWrapper(new Tunnel(80, jettyLocalPort, "localhost", "AUTO"));
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p);
    }

    public void testRunFromSlave() throws Exception {
        setCredential();

        Slave s = createSlave();

        FreeStyleProject p = createFreeStyleProject();
        p.setAssignedNode(s);
        SoDBuildWrapper before = new SoDBuildWrapper(new Tunnel(80, jettyLocalPort, "localhost", "test"+secret+".org"));
        p.getBuildWrappersList().add(before);
        invokeSeleniumFromBuild(p);
    }

    /**
     * Sets the SoD credential from ~/.sauce-ondemand.
     * For the tests to run, this file needs to be present.
     */
    private void setCredential() throws IOException {
        Credential c = new Credential();
        PluginImpl.get().setCredential(c.getUsername(),c.getKey());
    }

    /**
     * Executes a Selenium test as if it were run from the build.
     */
    private void invokeSeleniumFromBuild(FreeStyleProject p) throws Exception {
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                Credential c = new Credential();
                String h = build.getEnvironment(listener).get("SAUCE_ONDEMAND_HOST");
                DefaultSelenium selenium = new DefaultSelenium(
                            "saucelabs.com",
                            4444,
                            "{\"username\": \""+c.getUsername()+"\"," +
                            "\"access-key\": \""+c.getKey()+"\"," +
                            "\"os\": \"Windows 2003\"," +
                            "\"browser\": \"firefox\"," +
                            "\"browser-version\": \"3.\"," +
                            "\"job-name\": \"This is an example test\"}",
                            h!=null ? "http://"+h+'/' : "http://test"+secret+".org/");
                selenium.start();
                try {
                    selenium.open("/");
                    // if the server really hit our Jetty, we should see the same title that includes the secret code.
                    assertEquals("test"+secret,selenium.getTitle());
                } finally {
                    selenium.stop();
                }
                return true;
            }
        });

        buildAndAssertSuccess(p);
    }
}
