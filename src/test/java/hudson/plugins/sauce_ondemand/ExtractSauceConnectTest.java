package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import java.io.File;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ross Rowe
 */
public class ExtractSauceConnectTest {

    private SauceConnectFourManager manager = new SauceConnectFourManager();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void linux() throws Exception {
        File workingDirectory = new File(System.getProperty("java.io.tmpdir"));
        manager.extractZipFile(workingDirectory, SauceConnectFourManager.OperatingSystem.LINUX);
    }

    @Test
    public void windows() throws Exception {
        manager.extractZipFile(new File(System.getProperty("java.io.tmpdir")), SauceConnectFourManager.OperatingSystem.WINDOWS);
    }
}
