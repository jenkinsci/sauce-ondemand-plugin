package hudson.plugins.sauce_ondemand;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SauceOnDemandReportPublisherTest {

    @Test
    void testProcessSessionIds_none() {
        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null);
        assertTrue(details.isEmpty());
    }

    @Test
    void testProcessSessionIds_one() {
        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null, "SauceOnDemandSessionID=abc123 job-name=gavin");
        assertFalse(details.isEmpty());
        assertEquals(1, details.size());
        assertEquals("abc123", details.get(0).getJobId());
    }

    @Test
    void testProcessSessionIds_two() {
        LinkedList<TestIDDetails> details = SauceOnDemandReportPublisher.processSessionIds(null, "SauceOnDemandSessionID=abc123 job-name=gavin\n[firefox 32 OS X 10.10 #1-5] SauceOnDemandSessionID=941b498c5ad544dba92fe73fabfa9eb6 job-name=Insert Job Name Here");
        assertFalse(details.isEmpty());
        assertEquals(2, details.size());
        assertEquals("abc123", details.get(0).getJobId());
        assertEquals("941b498c5ad544dba92fe73fabfa9eb6", details.get(1).getJobId());
    }

}