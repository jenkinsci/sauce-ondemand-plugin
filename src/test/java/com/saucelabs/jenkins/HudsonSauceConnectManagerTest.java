package com.saucelabs.jenkins;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class HudsonSauceConnectManagerTest {

    private HudsonSauceConnectManager manager;

    @Before
    public void setUp() throws Exception {
        this.manager = new HudsonSauceConnectManager();
    }

    @Test
    public void defaultWorkingDirectoryIsHome() throws Exception {
        assertEquals(System.getProperty("user.home"), this.manager.getSauceConnectWorkingDirectory());
    }

    @Test
    public void emptyWorkingDirectoryIsHome() throws Exception {
        this.manager.setWorkingDirectory("");
        assertEquals(System.getProperty("user.home"), this.manager.getSauceConnectWorkingDirectory());
    }

    @Test
    public void specificWorkingDirectory() throws Exception {
        this.manager.setWorkingDirectory("/path");
        assertEquals("/path", this.manager.getSauceConnectWorkingDirectory());
    }
}
