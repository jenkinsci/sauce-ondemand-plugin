package com.saucelabs.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HudsonSauceConnectManagerTest {

    private HudsonSauceConnectManager manager;

    @BeforeEach
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
