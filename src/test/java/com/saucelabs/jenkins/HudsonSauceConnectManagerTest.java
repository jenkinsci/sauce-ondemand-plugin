package com.saucelabs.jenkins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudsonSauceConnectManagerTest {

    private HudsonSauceConnectManager manager;

    @BeforeEach
    void setUp() {
        manager = new HudsonSauceConnectManager();
    }

    @Test
    void defaultWorkingDirectoryIsHome() {
        assertEquals(System.getProperty("user.home"), manager.getSauceConnectWorkingDirectory());
    }

    @Test
    void emptyWorkingDirectoryIsHome() {
        manager.setWorkingDirectory("");
        assertEquals(System.getProperty("user.home"), manager.getSauceConnectWorkingDirectory());
    }

    @Test
    void specificWorkingDirectory() {
        manager.setWorkingDirectory("/path");
        assertEquals("/path", manager.getSauceConnectWorkingDirectory());
    }
}
