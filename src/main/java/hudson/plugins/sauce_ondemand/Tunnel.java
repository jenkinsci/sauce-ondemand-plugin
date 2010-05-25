package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Kohsuke Kawaguchi
 */
public class Tunnel extends AbstractDescribableImpl<Tunnel> {
    public final int remotePort;
    public final int localPort;
    public final String localHost;
    public final String domains;

    @DataBoundConstructor
    public Tunnel(int remotePort, int localPort, String localHost, String domains) {
        this.remotePort = remotePort;
        this.localHost = localHost;
        this.localPort = localPort;
        this.domains = domains.trim();
    }

    public List<String> getDomainList() {
        return asList(domains.split("\\s+"));
    }

    /**
     * If true, we'll generate the random remote host for each build.
     */
    public boolean isAutoRemoteHost() {
        return domains.equals("AUTO");
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Tunnel> {
        public String getDisplayName() {
            return "";
        }

        public FormValidation doCheckRemotePort(@QueryParameter String remotePort) {
            return FormValidation.validatePositiveInteger(remotePort);
        }

        public FormValidation doCheckLocalPort(@QueryParameter String remotePort) {
            return FormValidation.validatePositiveInteger(remotePort);
        }
    }
}
