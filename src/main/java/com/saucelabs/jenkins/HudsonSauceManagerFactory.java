package com.saucelabs.jenkins;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ross Rowe
 */
public class HudsonSauceManagerFactory {

    private static final HudsonSauceManagerFactory INSTANCE = new HudsonSauceManagerFactory();

    private Lock accessLock = new ReentrantLock();

    private SauceConnectFourManager sauceConnectFourManager;

    public static HudsonSauceManagerFactory getInstance() {
        return INSTANCE;
    }

    private HudsonSauceManagerFactory() {
    }

    public SauceConnectFourManager createSauceConnectFourManager() {
        accessLock.lock();
        try {
            if (sauceConnectFourManager == null)
            {
                sauceConnectFourManager = new HudsonSauceConnectFourManager();
            }
            return sauceConnectFourManager;
        } finally {
            accessLock.unlock();
        }
    }
}
