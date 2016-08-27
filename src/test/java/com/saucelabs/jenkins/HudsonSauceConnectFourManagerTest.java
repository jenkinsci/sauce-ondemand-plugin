package com.saucelabs.jenkins;

import com.saucelabs.jenkins.HudsonSauceConnectFourManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HudsonSauceConnectFourManagerTest {

    private HudsonSauceConnectFourManager manager;

    @Before
    public void setUp() throws Exception {
        this.manager = new HudsonSauceConnectFourManager();
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