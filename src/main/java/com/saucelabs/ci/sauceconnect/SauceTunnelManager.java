package com.saucelabs.ci.sauceconnect;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Interface which defines the behaviour for Sauce Connect Tunnel implementations.
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public interface SauceTunnelManager
{

    public void closeTunnelsForPlan(String planKey);

    public void addTunnelToMap(String planKey, Object tunnel);

    Object openConnection(String username, String apiKey) throws IOException;

    Map getTunnelMap();

    void setPrintStream(PrintStream logger);

    void setSauceConnectJar(File sauceConnectJar);
}
