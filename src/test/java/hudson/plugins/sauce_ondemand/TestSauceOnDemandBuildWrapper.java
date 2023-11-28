package hudson.plugins.sauce_ondemand;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.AbstractBuild;

@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
class TestSauceOnDemandBuildWrapper extends SauceOnDemandBuildWrapper {

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

  @Extension
  public static class DescriptorImpl extends SauceOnDemandBuildWrapper.DescriptorImpl {}
}
