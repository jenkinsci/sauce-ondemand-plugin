package hudson.plugins.sauce_ondemand;

import hudson.Extension;

class TestSauceOnDemandBuildWrapper extends SauceOnDemandBuildWrapper {
    @Extension
    public static class DescriptorImpl extends SauceOnDemandBuildWrapper.DescriptorImpl {
    }

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
                null,
                null,
                null,
//                false,
                false);
    }
}
