package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ross Rowe
 */
class ExtractSauceConnectTest {

    private final SauceConnectManager manager = new SauceConnectManager();

    @Test
    void linux() throws Exception {
        File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
        File file = manager.extractZipFile(workingDirectory, SauceConnectManager.OperatingSystem.LINUX_AMD64, NOPLogger.NOP_LOGGER);
        assertTrue(FileUtils.deleteQuietly(file));
    }

    @Test
    void windows() throws Exception {
        File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
        File file = manager.extractZipFile(workingDirectory, SauceConnectManager.OperatingSystem.WINDOWS_AMD64, NOPLogger.NOP_LOGGER);
        assertTrue(FileUtils.deleteQuietly(file));
    }
}
