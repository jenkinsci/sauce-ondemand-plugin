package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.sauceconnect.SauceConnectManager;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.helpers.NOPLogger;

/**
 * @author Ross Rowe
 */
public class ExtractSauceConnectTest {

    private SauceConnectManager manager = new SauceConnectManager();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void linux() throws Exception {
        File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
        manager.extractZipFile(workingDirectory, SauceConnectManager.OperatingSystem.LINUX_AMD64, NOPLogger.NOP_LOGGER);
    }

    @Test
    public void windows() throws Exception {
        manager.extractZipFile(new File(System.getProperty("java.io.tmpdir")), SauceConnectManager.OperatingSystem.WINDOWS_AMD64, NOPLogger.NOP_LOGGER);
    }
}
