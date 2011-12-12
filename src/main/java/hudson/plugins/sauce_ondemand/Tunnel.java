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
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;

/**
 * A tunnel configuration. Previously (for Sauce Connect v1), the tunnel supported the storage
 * of local/remote hosts and ports and domains.  As of Sauce Connect v2, the need for remote hosts
 * and ports is removed.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Tunnel extends AbstractDescribableImpl<Tunnel> implements Serializable {
    public final int localPort;
    public final String localHost;
    public final String startingURL;

    @DataBoundConstructor
    public Tunnel(int localPort, String localHost, String startingURL) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.startingURL = startingURL;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Tunnel> {
        public String getDisplayName() {
            return "";
        }

        public FormValidation doCheckLocalPort(@QueryParameter String remotePort) {
            if (StringUtils.isBlank(remotePort)) {
                return FormValidation.ok();
            }
            else {
               return FormValidation.validatePositiveInteger(remotePort);
            }

        }
    }

    private static final long serialVersionUID = 1L;
}
