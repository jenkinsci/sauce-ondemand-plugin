package com.saucelabs.jenkins;

import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ross Rowe
 */
public class HudsonSauceManagerFactory {

    private static final HudsonSauceManagerFactory INSTANCE = new HudsonSauceManagerFactory();

    private Lock accessLock = new ReentrantLock();

    private SauceConnectManager sauceConnectManager;

    public static HudsonSauceManagerFactory getInstance() {
        return INSTANCE;
    }

    private HudsonSauceManagerFactory() {
    }

    public SauceConnectManager createSauceConnectManager() {
        accessLock.lock();
        try {
            if (sauceConnectManager == null)
            {
                sauceConnectManager = new HudsonSauceConnectManager();
            }
            return sauceConnectManager;
        } finally {
            accessLock.unlock();
        }
    }
}
