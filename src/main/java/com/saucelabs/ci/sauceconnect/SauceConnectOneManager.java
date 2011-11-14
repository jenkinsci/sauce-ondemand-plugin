package com.saucelabs.ci.sauceconnect;

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.SauceTunnel;
import com.saucelabs.rest.SauceTunnelFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles opening a SSH Tunnel using the Sauce Connect One logic (now considered to be legacy). The class  maintains a cache of {@link com.saucelabs.rest.SauceTunnel} instances mapped against
 * the corresponding plan key.  This class can be considered a singleton, and is instantiated via the 'component' element of the atlassian-plugin.xml
 * file (ie. using Spring).
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class SauceConnectOneManager implements SauceTunnelManager {
    private static final Logger logger = Logger.getLogger(SauceConnectOneManager.class);

    private Map<String, List<SauceTunnel>> tunnelMap;

    public SauceConnectOneManager() {
        this.tunnelMap = new HashMap<String, List<SauceTunnel>>();
    }

    /**
     * @param planKey
     */
    public void closeTunnelsForPlan(String planKey) {
        if (tunnelMap.containsKey(planKey)) {
            List<SauceTunnel> tunnelList = tunnelMap.get(planKey);
            for (SauceTunnel tunnel : tunnelList) {
                try {
                    tunnel.disconnectAll();
                    tunnel.destroy();
                } catch (IOException e) {
                    logger.error("Failed to close a Sauce OnDemand Tunnel");
                    //continue processing
                }
            }

            tunnelMap.remove(planKey);
        }

    }

    /**
     * @param planKey
     * @param tunnel
     */
    public void addTunnelToMap(String planKey, Object tunnel) {
        if (!tunnelMap.containsKey(planKey)) {
            tunnelMap.put(planKey, new ArrayList<SauceTunnel>());
        }

        tunnelMap.get(planKey).add((SauceTunnel) tunnel);
    }

    /**
     * Opens a new SSH Tunnel.
     * 
     * @param username
     * @param apiKey
     * @param localHost
     * @param intLocalPort
     * @param intRemotePort
     * @param domainList
     * @return
     * @throws IOException
     */
    public Object openConnection(String username, String apiKey, String localHost, int intLocalPort, int intRemotePort, String domain) throws IOException {

        SauceTunnelFactory tunnelFactory = new SauceTunnelFactory(new Credential(username, apiKey));
        SauceTunnel tunnel = tunnelFactory.create(Collections.singletonList(domain));

        if (tunnel != null) {
            try {
                tunnel.waitUntilRunning(SSH_TIMEOUT);
                if (!tunnel.isRunning()) {
                    throw new IOException("Sauce OnDemand Tunnel didn't come online. Aborting.");
                }
            } catch (InterruptedException e) {
                throw new IOException("Sauce OnDemand Tunnel Aborted.");
            }
            tunnel.connect(intRemotePort, localHost, intLocalPort);
        }
        return tunnel;
    }

    public Map getTunnelMap() {
        return tunnelMap;
    }
}