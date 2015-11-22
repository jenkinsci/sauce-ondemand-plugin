package com.saucelabs.hudson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by gavinmogan on 2015-11-21.
 */
public class HudsonSauceConnectFourManagerTest {

    private HudsonSauceConnectFourManager manager;
    
    @Test
    public void test() throws Exception {
        this.manager = new HudsonSauceConnectFourManager();
        assertEquals(System.getProperty("user.home"), this.manager.getSauceConnectWorkingDirectory());

        this.manager.setWorkingDirectory("");
        assertEquals(System.getProperty("user.home"), this.manager.getSauceConnectWorkingDirectory());

        this.manager.setWorkingDirectory("/path");
        assertEquals("/path", this.manager.getSauceConnectWorkingDirectory());
    }
}