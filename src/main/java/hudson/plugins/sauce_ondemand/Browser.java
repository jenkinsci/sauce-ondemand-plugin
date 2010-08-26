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

/**
 * Type-safe enums for browsers that are available on Sauce OnDemand.
 * 
 * @author Kohsuke Kawaguchi
 */
public enum Browser {
    Firefox2_0_Windows2003("Windows 2003","firefox","2.0.",             "Firefox 2.0 on Windows 2003"),
    Firefox3_0_Windows2003("Windows 2003","firefox","3.0.",             "Firefox 3.0 on Windows 2003"),
    Firefox3_5_Windows2003("Windows 2003","firefox","3.5.",             "Firefox 3.5 on Windows 2003"),
    Firefox3_6_Windows2003("Windows 2003","firefox","3.6.",             "Firefox 3.6 on Windows 2003"),
    Firefox2_0_proxy_Windows2003("Windows 2003","firefoxproxy","2.0.",  "Firefox 2.0 on Windows 2003 [proxy mode]"),
    Firefox3_0_proxy_Windows2003("Windows 2003","firefoxproxy","3.0.",  "Firefox 3.0 on Windows 2003 [proxy mode]"),
    Firefox3_5_proxy_Windows2003("Windows 2003","firefoxproxy","3.5.",  "Firefox 3.5 on Windows 2003 [proxy mode]"),
    Firefox3_6_proxy_Windows2003("Windows 2003","firefoxproxy","3.6.",  "Firefox 3.6 on Windows 2003 [proxy mode]"),
    GoogleChrome_Windows2003("Windows 2003","googlechrome","",          "Google Chrome on Windows 2003"),
    IE6("Windows 2003","iexplore","6.",                                 "Internet Explorer 6 on Windows 2003"),
    IE7("Windows 2003","iexplore","7.",                                 "Internet Explorer 7 on Windows 2003"),
    IE8("Windows 2003","iexplore","8.",                                 "Internet Explorer 8 on Windows 2003"),
    IE6_proxy("Windows 2003","iexploreproxy","6.",                      "Internet Explorer 6 on Windows 2003 [proxy mode]"),
    IE7_proxy("Windows 2003","iexploreproxy","7.",                      "Internet Explorer 7 on Windows 2003 [proxy mode]"),
    IE8_proxy("Windows 2003","iexploreproxy","8.",                      "Internet Explorer 8 on Windows 2003 [proxy mode]"),
    Opera9("Windows 2003","opera","9.",                                 "Opera 9 on Windows 2003"),
    Opera10("Windows 2003","opera","10.",                               "Opera 10 on Windows 2003"),
    Safari3("Windows 2003","safari","3.",                               "Safari 3 on Windows 2003"),
    Safari4("Windows 2003","safari","4.",                               "Safari 4 on Windows 2003"),
    Safari5("Windows 2003","safari","5.",                               "Safari 5 on Windows 2003"),
    Safari3_proxy("Windows 2003","safari","3.",                         "Safari 3 on Windows 2003 [proxy mode]"),
    Safari4_proxy("Windows 2003","safari","4.",                         "Safari 4 on Windows 2003 [proxy mode]"),
    Safari5_proxy("Windows 2003","safari","5.",                         "Safari 5 on Windows 2003 [proxy mode]"),

    Firefox3_0_Linux("Linux","firefox","3.0.",                          "Firefox 3.0 on Linux"),
    Firefox3_0_proxy_Linux("Linux","firefoxproxy","3.0.",               "Firefox 3.0 on Linux [proxy mode]"),
    ;

    public final String browser;
    public final String browser_version;
    public final String os;
    public final String displayName;

    Browser(String os, String browser, String browser_version, String displayName) {
        this.browser = browser;
        this.browser_version = browser_version;
        this.os = os;
        this.displayName = displayName;
    }

    /**
     * Returns the driver URI string.
     */
    public String getUri() {
        return "sauce-ondemand:?os="+os+"&browser="+browser+"&browser-version="+browser_version;
    }
}

