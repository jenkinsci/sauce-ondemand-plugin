import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;

/**
 * @author Ross Rowe
 */
public class LaunchSauceConnect {

    private SauceConnectTwoManager manager = new SauceConnectTwoManager();

    public void setUp() throws Exception {
        manager.openConnection("rossco_9_9", "44f0744c-1689-4418-af63-560303cbb37b", 4445, null, null, null);
    }

    public void tearDown() throws Exception {
        manager.closeTunnelsForPlan("rossco_9_9", null);
    }
}
