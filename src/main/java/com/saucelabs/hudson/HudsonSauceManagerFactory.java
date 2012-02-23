package com.saucelabs.hudson;

import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ross Rowe
 */
public class HudsonSauceManagerFactory {

    private static final HudsonSauceManagerFactory INSTANCE = new HudsonSauceManagerFactory();

    private PlexusContainer plexus = null;

    private Lock accessLock = new ReentrantLock();

    public static HudsonSauceManagerFactory getInstance() {
        return INSTANCE;
    }

    private HudsonSauceManagerFactory() {
    }

    public SauceConnectTwoManager createSauceConnectManager() throws ComponentLookupException {
        accessLock.lock();
        try {
            start();
            return (SauceConnectTwoManager) this.plexus.lookup(SauceTunnelManager.class.getName());
        } finally {
            accessLock.unlock();
        }
    }

    public void start() {
        if (plexus == null) {
            try {
                this.plexus = new DefaultPlexusContainer();
            } catch (PlexusContainerException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                // These will only be useful for Hudson v1.395 and under
                // ... Since the use of sisu-plexus-inject will initialize
                // everything in the constructor
                PlexusContainer.class.getDeclaredMethod("initialize").invoke(this.plexus);
                PlexusContainer.class.getDeclaredMethod("start").invoke(this.plexus);
            } catch (Throwable e) { /* Don't do anything here ... initialize/start methods should be called prior to v1.395 ! */ }
        }
    }
}
