package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.AbstractBuild;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TestSauceOnDemandBuildWrapper extends SauceOnDemandBuildWrapper {

    private final JenkinsSauceREST mockSauceREST;

    public TestSauceOnDemandBuildWrapper(String credentialId, JenkinsSauceREST sauceREST) {
        super(
            true,
            null,
            credentialId,
            new SeleniumInformation(null, null),
            null,
            null,
            "",
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
        this.mockSauceREST = sauceREST;
    }

    public TestSauceOnDemandBuildWrapper(String credentialId) {
        this(credentialId, null);
    }

    @Override
    public SauceOnDemandBuildAction getSauceBuildAction(AbstractBuild build) {
        return new SauceOnDemandBuildAction(build, getCredentialId()) {
            @Override
            protected JenkinsSauceREST getSauceREST() {
                return mockSauceREST;
            }
        };
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Extension
    public static class DescriptorImpl extends SauceOnDemandBuildWrapper.DescriptorImpl {
    }
}