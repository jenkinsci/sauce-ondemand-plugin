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

import com.saucelabs.ci.BrowserFactory;
import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link Axis} that configures {@code SELENIUM_DRIVER}.
 * @author Kohsuke Kawaguchi
 */
public class BrowserAxis extends Axis {
    
    private static final Logger logger = Logger.getLogger(BrowserAxis.class);
    
    @DataBoundConstructor
    public BrowserAxis(List<String> values) {
        super("SELENIUM_DRIVER", values);
    }

    public boolean hasValue(String v) {
        return getValues().contains(v);
    }

    // TODO: more hooks to inject variables and values
    // TODO: matrix or a list as the UI?

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand Cross-browser tests";
        }

        public List<com.saucelabs.ci.Browser> getBrowsers() {
            try {
                return BrowserFactory.getInstance().values();
            } catch (IOException e) {
                logger.error("Error retrieving browsers from Saucelabs", e);
            } catch (JSONException e) {
                logger.error("Error parsing JSON response", e);
            }
            return Collections.emptyList();
        }
    }

    public void addBuildVariable(String value, Map<String,String> map) {
        com.saucelabs.ci.Browser b = BrowserFactory.getInstance().forKey(value);
        if (b!=null)    // should never be null, but let's be defensive in case of downgrade.
            map.put(getName(),b.getUri());
    }
}
