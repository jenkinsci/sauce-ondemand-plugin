package hudson.plugins.sauce_ondemand;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;

@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
class TestSauceOnDemandBuildWrapper extends SauceOnDemandBuildWrapper {
    public TestSauceOnDemandBuildWrapper(String credentialId) {
        super(
                true,
                null,
                credentialId,
                new SeleniumInformation(null, null),
                null,
                null,
                "",
                null,
                false,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                false);
    }

    @Extension
    public static class DescriptorImpl extends SauceOnDemandBuildWrapper.DescriptorImpl {
    }
}
