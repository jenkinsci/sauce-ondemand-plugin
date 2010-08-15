/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;
import java.util.List;

import static java.util.Arrays.*;

/**
 * A tunnel configuration.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Tunnel extends AbstractDescribableImpl<Tunnel> implements Serializable {
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

    private static final long serialVersionUID = 1L;
}
