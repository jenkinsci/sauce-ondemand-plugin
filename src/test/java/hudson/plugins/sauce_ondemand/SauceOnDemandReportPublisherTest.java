package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import junit.framework.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SauceOnDemandReportPublisherTest {

    @Test
    public void testProcessSessionIds_none() throws Exception {
        SauceOnDemandBuildAction sauceOnDemandBuildAction;

        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null, new String[]{});
        assertTrue(details.isEmpty());
    }

    @Test
    public void testProcessSessionIds_one() throws Exception{
        SauceOnDemandBuildAction sauceOnDemandBuildAction;
        List<JenkinsJobInformation> jobs;

        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null, new String[]{
            "SauceOnDemandSessionID=abc123 job-name=gavin"
        });
        assertFalse(details.isEmpty());
        assertEquals(1, details.size());
        assertEquals("abc123", details.get(0).getJobId());
    }

    @Test
    public void testProcessSessionIds_two() throws Exception {
        SauceOnDemandBuildAction sauceOnDemandBuildAction;
        List<JenkinsJobInformation> jobs;

        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null, new String[]{
            "SauceOnDemandSessionID=abc123 job-name=gavin\n[firefox 32 OS X 10.10 #1-5] SauceOnDemandSessionID=941b498c5ad544dba92fe73fabfa9eb6 job-name=Insert Job Name Here"
        });
        assertFalse(details.isEmpty());
        assertEquals(2, details.size());
        assertEquals("abc123", details.get(0).getJobId());
        assertEquals("941b498c5ad544dba92fe73fabfa9eb6", details.get(1).getJobId());
    }

}